package io.github.thiagojosetj.portfolio.management.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.engine.jdbc.connections.internal.UserSuppliedConnectionProviderImpl;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.Query;

/**
 * Checks HQL/constructor compatibility without a database; PostgreSQL execution is tested apart.
 */
class PortfolioSnapshotQueryTest {

  @Test
  void validatesTheScalarLeftJoinAgainstTheActualHibernateMappings() throws Exception {
    String query =
        PortfolioJpaRepository.class
            .getMethod("findOwnedSnapshotRows", UUID.class, UUID.class)
            .getAnnotation(Query.class)
            .value();
    var registry =
        new StandardServiceRegistryBuilder()
            .applySetting("hibernate.boot.allow_jdbc_metadata_access", false)
            .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
            .applySetting("hibernate.hbm2ddl.auto", "none")
            .applySetting(
                "hibernate.connection.provider_class", UserSuppliedConnectionProviderImpl.class)
            .build();
    try (var factory =
            new MetadataSources(registry)
                .addAnnotatedClass(PortfolioJpaEntity.class)
                .addAnnotatedClass(AllocationClassJpaEntity.class)
                .buildMetadata()
                .buildSessionFactory();
        var session = factory.openSession()) {
      var selection = session.createSelectionQuery(query, OwnedPortfolioSnapshotRow.class);
      selection.setParameter("ownerUserId", UUID.randomUUID());
      selection.setParameter("portfolioId", UUID.randomUUID());
      assertThat(selection).isNotNull();
    } finally {
      StandardServiceRegistryBuilder.destroy(registry);
    }
  }
}
