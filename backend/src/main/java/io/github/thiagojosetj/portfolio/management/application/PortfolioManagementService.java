package io.github.thiagojosetj.portfolio.management.application;

import io.github.thiagojosetj.portfolio.management.application.PortfolioView.AllocationTargetView;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetDefinition;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetReplacement;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetSet;
import io.github.thiagojosetj.portfolio.management.domain.AllocationTargetUpdate;
import io.github.thiagojosetj.portfolio.management.domain.PortfolioValidationException;
import io.github.thiagojosetj.portfolio.management.persistence.AllocationClassJpaEntity;
import io.github.thiagojosetj.portfolio.management.persistence.AllocationClassJpaRepository;
import io.github.thiagojosetj.portfolio.management.persistence.PortfolioJpaEntity;
import io.github.thiagojosetj.portfolio.management.persistence.PortfolioJpaRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("!simulator & !demo")
public class PortfolioManagementService {

  private static final int MAX_PORTFOLIO_NAME_LENGTH = 100;

  private final PortfolioJpaRepository portfolioRepository;
  private final AllocationClassJpaRepository allocationClassRepository;
  private final Clock clock;

  public PortfolioManagementService(
      PortfolioJpaRepository portfolioRepository,
      AllocationClassJpaRepository allocationClassRepository,
      Clock clock) {
    this.portfolioRepository = portfolioRepository;
    this.allocationClassRepository = allocationClassRepository;
    this.clock = clock;
  }

  @Transactional
  public PortfolioView createPortfolio(CreatePortfolioCommand command) {
    if (command == null || command.ownerUserId() == null) {
      throw invalid("ownerUserId", "required", "O proprietário da carteira é obrigatório.");
    }

    String name = normalizePortfolioName(command.name());
    AllocationTargetSet targetSet = AllocationTargetSet.from(command.allocationTargets());

    if (!portfolioRepository.ownerExists(command.ownerUserId())) {
      throw invalid("ownerUserId", "not_found", "O proprietário da carteira não existe.");
    }
    Instant now = databaseTimestamp();
    UUID portfolioId = UUID.randomUUID();
    PortfolioJpaEntity portfolio =
        PortfolioJpaEntity.create(portfolioId, command.ownerUserId(), name, now);
    try {
      portfolioRepository.save(portfolio);
      portfolioRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      if (causedByConstraint(exception, "uq_portfolio_owner_name_ci")) {
        throw invalid("name", "duplicate", "Já existe uma carteira com esse nome.");
      }
      throw exception;
    }

    persistTargets(portfolioId, targetSet.targets(), now);
    return loadOwnedPortfolio(command.ownerUserId(), portfolioId);
  }

  @Transactional(readOnly = true)
  public PortfolioView findPortfolio(UUID ownerUserId, UUID portfolioId) {
    requireIdentifiers(ownerUserId, portfolioId);
    return loadOwnedPortfolio(ownerUserId, portfolioId);
  }

  @Transactional
  public PortfolioView replaceAllocationTargets(ReplaceAllocationTargetsCommand command) {
    if (command == null) {
      throw invalid("command", "required", "A alteração da carteira é obrigatória.");
    }
    requireIdentifiers(command.ownerUserId(), command.portfolioId());
    if (command.expectedPortfolioVersion() < 0
        || command.expectedPortfolioVersion() == Long.MAX_VALUE) {
      throw invalid(
          "expectedPortfolioVersion", "range", "A versão esperada da carteira é inválida.");
    }

    AllocationTargetReplacement replacement =
        AllocationTargetReplacement.from(command.allocationTargets());
    Instant now = databaseTimestamp();
    int claimed =
        portfolioRepository.claimOwnedVersion(
            command.ownerUserId(), command.portfolioId(), command.expectedPortfolioVersion(), now);

    if (claimed == 0) {
      if (portfolioRepository.existsByIdAndOwnerUserId(
          command.portfolioId(), command.ownerUserId())) {
        throw new PortfolioVersionConflictException();
      }
      throw new PortfolioNotFoundException();
    }

    replaceOwnedTargets(command.ownerUserId(), command.portfolioId(), replacement.targets(), now);
    return loadOwnedPortfolio(command.ownerUserId(), command.portfolioId());
  }

  private void replaceOwnedTargets(
      UUID ownerUserId, UUID portfolioId, List<AllocationTargetUpdate> updates, Instant now) {
    // The CAS clears the persistence context; load managed rows only after claiming the version.
    List<AllocationClassJpaEntity> current =
        allocationClassRepository.findAllOwnedByPortfolioId(ownerUserId, portfolioId);
    Map<UUID, AllocationClassJpaEntity> currentById = new HashMap<>();
    current.forEach(target -> currentById.put(target.getId(), target));
    Set<UUID> retainedIds = new HashSet<>();
    for (AllocationTargetUpdate update : updates) {
      if (update.id() != null) {
        if (!currentById.containsKey(update.id())) {
          throw invalid(
              "allocationTargets.id", "not_found", "Uma classe informada não pertence à carteira.");
        }
        retainedIds.add(update.id());
      }
    }

    List<AllocationClassJpaEntity> removed =
        current.stream().filter(target -> !retainedIds.contains(target.getId())).toList();
    allocationClassRepository.deleteAll(removed);
    allocationClassRepository.flush();

    stageRetainedTargets(current, updates, retainedIds);
    allocationClassRepository.flush();

    List<AllocationClassJpaEntity> finalTargets = new ArrayList<>(updates.size());
    for (int index = 0; index < updates.size(); index++) {
      AllocationTargetUpdate update = updates.get(index);
      AllocationClassJpaEntity target;
      if (update.id() == null) {
        target =
            AllocationClassJpaEntity.create(
                UUID.randomUUID(),
                portfolioId,
                update.name(),
                (short) index,
                update.targetPercentage(),
                now);
      } else {
        target = currentById.get(update.id());
        target.updateTarget(update.name(), (short) index, update.targetPercentage(), now);
      }
      finalTargets.add(target);
    }
    saveTargets(finalTargets);
  }

  private void stageRetainedTargets(
      List<AllocationClassJpaEntity> current,
      List<AllocationTargetUpdate> updates,
      Set<UUID> retainedIds) {
    // Immediate unique indexes reject direct name/order swaps. Park retained rows inside the
    // transaction, preserving their IDs/createdAt and avoiding every old/final name and order.
    // Numeric sentinels have no case-folding ambiguity with the lower(name) unique index.
    Set<String> occupiedNames = new HashSet<>();
    Set<Integer> occupiedOrders = new HashSet<>();
    current.forEach(
        target -> {
          occupiedNames.add(target.getName());
          occupiedOrders.add(target.getDisplayOrder().intValue());
        });
    for (int index = 0; index < updates.size(); index++) {
      occupiedNames.add(updates.get(index).name());
      occupiedOrders.add(index);
    }

    int nameSuffix = 0;
    int temporaryOrder = 0;
    for (AllocationClassJpaEntity target : current) {
      if (!retainedIds.contains(target.getId())) {
        continue;
      }
      String temporaryName;
      do {
        temporaryName = "~" + nameSuffix++ + "~";
      } while (!occupiedNames.add(temporaryName));
      while (occupiedOrders.contains(temporaryOrder)) {
        temporaryOrder++;
      }
      // At most 20 current and 20 final orders are reserved, well within SMALLINT.
      occupiedOrders.add(temporaryOrder);
      target.stageReplacement(temporaryName, (short) temporaryOrder++);
    }
  }

  private void persistTargets(
      UUID portfolioId, List<AllocationTargetDefinition> targets, Instant now) {
    List<AllocationClassJpaEntity> entities =
        java.util.stream.IntStream.range(0, targets.size())
            .mapToObj(
                index -> {
                  AllocationTargetDefinition target = targets.get(index);
                  return AllocationClassJpaEntity.create(
                      UUID.randomUUID(),
                      portfolioId,
                      target.name(),
                      (short) index,
                      target.targetPercentage(),
                      now);
                })
            .toList();
    saveTargets(entities);
  }

  private void saveTargets(List<AllocationClassJpaEntity> targets) {
    try {
      allocationClassRepository.saveAll(targets);
      allocationClassRepository.flush();
    } catch (DataIntegrityViolationException exception) {
      if (causedByConstraint(exception, "uq_allocation_class_portfolio_name_ci")) {
        throw invalid(
            "allocationTargets", "duplicate", "Os nomes das classes devem ser únicos na carteira.");
      }
      throw exception;
    }
  }

  private PortfolioView loadOwnedPortfolio(UUID ownerUserId, UUID portfolioId) {
    PortfolioJpaEntity portfolio =
        portfolioRepository
            .findOwnedById(ownerUserId, portfolioId)
            .orElseThrow(PortfolioNotFoundException::new);
    List<AllocationTargetView> targets =
        allocationClassRepository.findAllOwnedByPortfolioId(ownerUserId, portfolioId).stream()
            .map(
                target ->
                    new AllocationTargetView(
                        target.getId(),
                        target.getName(),
                        target.getDisplayOrder(),
                        target.getTargetPercentage(),
                        target.getCreatedAt(),
                        target.getUpdatedAt()))
            .toList();

    return new PortfolioView(
        portfolio.getId(),
        portfolio.getOwnerUserId(),
        portfolio.getName(),
        portfolio.getBaseCurrency(),
        portfolio.getVersion(),
        portfolio.getCreatedAt(),
        portfolio.getUpdatedAt(),
        targets);
  }

  private static void requireIdentifiers(UUID ownerUserId, UUID portfolioId) {
    if (ownerUserId == null) {
      throw invalid("ownerUserId", "required", "O proprietário da carteira é obrigatório.");
    }
    if (portfolioId == null) {
      throw invalid("portfolioId", "required", "A carteira é obrigatória.");
    }
  }

  private static String normalizePortfolioName(String candidate) {
    if (candidate == null) {
      throw invalid("name", "required", "O nome da carteira é obrigatório.");
    }
    String normalized = candidate.strip();
    if (normalized.isEmpty() || normalized.length() > MAX_PORTFOLIO_NAME_LENGTH) {
      throw invalid("name", "length", "O nome da carteira deve ter entre 1 e 100 caracteres.");
    }
    return normalized;
  }

  private static PortfolioValidationException invalid(String field, String code, String message) {
    return new PortfolioValidationException(field, code, message);
  }

  private Instant databaseTimestamp() {
    return clock.instant().truncatedTo(ChronoUnit.MICROS);
  }

  private static boolean causedByConstraint(Throwable failure, String constraintName) {
    Throwable cause = failure;
    while (cause != null) {
      if (cause instanceof ConstraintViolationException constraintViolation
          && constraintName.equals(constraintViolation.getConstraintName())) {
        return true;
      }
      cause = cause.getCause();
    }
    return false;
  }
}
