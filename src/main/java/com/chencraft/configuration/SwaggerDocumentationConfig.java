package com.chencraft.configuration;

import com.chencraft.model.CertificatePEM;
import com.chencraft.model.ErrorResponse;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.SpecVersion;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.Map;

import static com.chencraft.api.models.ResponseConstants.*;
import static com.chencraft.api.models.TagConstants.*;

@jakarta.annotation.Generated(value = "io.swagger.codegen.v3.generators.java.SpringCodegen", date = "2025-08-09T11:04:45.539601364Z[Etc/UTC]")
@Configuration
public class SwaggerDocumentationConfig {
    @Value("${app.swagger.server.url:}")
    private String serverUrl;

    @Value("${app.swagger.server.desc:}")
    private String serverDescription;

    // Version will be replaced when deployed in GitHub
    @Value("${info.app.version:0.0.1-TEST}")
    private String appVersion;

    @Bean
    public OpenAPI openApi() {
        return new OpenAPI()
                .specVersion(SpecVersion.V31)
                .info(new Info()
                              .title("Sibei's Personal API Server - OpenAPI 3.1")
                              .description(
                                      """
                                              My personal API server based on the OpenAPI 3.1 specification, created using [https://swagger.io](https://swagger.io).
                                              
                                              Some useful links:
                                               - [API server repository](https://github.com/SibeiC/api-server)
                                               - [Swagger UI repository](https://github.com/swagger-api/swagger-ui)""")
                              .termsOfService("https://swagger.io/terms/")
                              .version(appVersion)
                              .license(new License()
                                               .name("GPL-3.0")
                                               .url("https://www.gnu.org/licenses/gpl-3.0.html"))
                              .contact(new Contact()
                                               .email("api@chencraft.com")))
                .externalDocs(new ExternalDocumentation().description("Find out more about springdoc-openapi-starter-webmvc-ui")
                                                         .url("https://springdoc.org/"))
                .addServersItem(new Server().url(serverUrl).description(serverDescription))
                .tags(generateTags())
                .components(generateComponents());
    }

    private static List<Tag> generateTags() {
        return List.of(new Tag().name(FILE).description("Serves file from cloud storage"),
                       new Tag().name(WEBHOOK).description("Handles webhook requests")
                                .externalDocs(new ExternalDocumentation().description("GitHub Webhooks Docs")
                                                                         .url("https://docs.github.com/en/webhooks")),
                       new Tag().name(TLS).description("Handles client TLS certificate issue and renewal"),
                       new Tag().name(CLOUDFLARE).description("Common Cloudflare API bundles")
                                .externalDocs(new ExternalDocumentation().description("Cloudflare API Docs")
                                                                         .url("https://developers.cloudflare.com/api/")),
                       new Tag().name(GITHUB).description("Common GitHub API bundles")
                                .externalDocs(new ExternalDocumentation().description("GitHub API Docs")
                                                                         .url("https://docs.github.com/en/rest/")));
    }

    private static Components generateComponents() {
        Components components = new Components();
        schemaForRequests(components);
        schemaFor2xxResponses(components);
        schemaFor4xxResponses(components);
        schemaFor5xxResponses(components);
        return components;
    }

    private static void schemaForRequests(Components components) {
    }

    private static void schemaFor2xxResponses(Components components) {
        addSchemas(components, CertificatePEM.class);
        components.addResponses(OK_RESPONSE, new ApiResponse()
                          .description("OK")
                          .content(new Content().addMediaType(org.springframework.http.MediaType.TEXT_PLAIN_VALUE, new MediaType())))
                  .addResponses(OK_FILE_RESPONSE, new ApiResponse()
                          .description("File being returned")
                          .content(new Content().addMediaType("application/*", new MediaType())))
                  .addResponses(OK_PEM_RESPONSE, new ApiResponse()
                          .description("Returns freshly issued certificate and private key")
                          .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, new MediaType()
                                                        .schema(new Schema<CertificatePEM>().$ref("#/components/schemas/CertificatePEM")))
                                                .addMediaType("application/x-pem-file", new MediaType())));
    }

    private static void schemaFor4xxResponses(Components components) {
        components.addResponses(INVALID_INPUT_RESPONSE, new ApiResponse()
                          .description("Invalid request")
                          .content(null))
                  .addResponses(UNAUTHORIZED_RESPONSE, new ApiResponse()
                          .description("Authorization information is missing or invalid")
                          .content(null))
                  .addResponses(FORBIDDEN_RESPONSE, new ApiResponse()
                          .description("You shall not pass")
                          .content(null))
                  .addResponses(SIGNATURE_INVALID_RESPONSE, new ApiResponse()
                          .description("Could not verify with the signature provided")
                          .content(null))
                  .addResponses(FILE_NOT_FOUND_RESPONSE, new ApiResponse()
                          .description("File not found")
                          .content(null));
    }

    private static void schemaFor5xxResponses(Components components) {
        addSchemas(components, ErrorResponse.class);
        components.addResponses(INTERNAL_SERVER_ERROR_RESPONSE, new ApiResponse()
                .description("Unexpected error")
                .content(new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE, new MediaType()
                        .schema(new Schema<ErrorResponse>().$ref("#/components/schemas/ErrorResponse")))));
    }

    private static void addSchemas(Components components, Class<?> schemaClass) {
        Map<String, Schema> schemas = ModelConverters.getInstance().read(schemaClass);
        schemas.forEach(components::addSchemas);
    }
}
