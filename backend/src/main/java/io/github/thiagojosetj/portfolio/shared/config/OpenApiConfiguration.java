package io.github.thiagojosetj.portfolio.shared.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
    info =
        @Info(
            title = "Planejador de Carteira API",
            version = "v1",
            description =
                "API educacional para simulação determinística de novos aportes. "
                    + "Não constitui recomendação de investimento.",
            contact = @Contact(name = "Thiago Jose", url = "https://github.com/thiagojosetj"),
            license = @License(name = "MIT License", url = "https://opensource.org/license/mit")))
public class OpenApiConfiguration {}
