/*
 * Database-related beans for the Spring context
 */
package uk.ac.antarctica.helpdesk.config;

import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseConfig {

    @Bean
    @Primary
    @ConfigurationProperties(prefix = "datasource.magic")
    public DataSource magicDataSource() {
        return DataSourceBuilder.create().build();
    }

    @Bean
    public JdbcTemplate magicDataTpl() {
        return (new JdbcTemplate(magicDataSource()));
    }

}
