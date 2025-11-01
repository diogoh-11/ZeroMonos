package tqs.zeromonos.functionals;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("tqs/zeromonos") // Os arquivos .feature devem estar em src/test/resources/tqs/zeromonos
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "tqs.zeromonos.functionals") // Os testes devem estar em tqs.zeromonos.functionals
public class RunCucumberTest {
}
