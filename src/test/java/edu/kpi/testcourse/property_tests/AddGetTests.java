package edu.kpi.testcourse.property_tests;
import edu.kpi.testcourse.dataservice.DataService;
import edu.kpi.testcourse.dataservice.UrlAlias;
import edu.kpi.testcourse.dataservice.User;
import edu.kpi.testcourse.urlservice.AliasInfo;
import edu.kpi.testcourse.urlservice.UrlService;
import static org.quicktheories.generators.SourceDSL.strings;
import io.micronaut.context.BeanContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.quicktheories.QuickTheory.qt;

@MicronautTest
public class AddGetTests {
  @Inject DataService dataService;
  @Inject UrlService urlService;

  @BeforeEach
  void beforeEach() {
    dataService.clear();
    dataService.addUser(new User("jusovch", "j2202"));
  }

  @Test
  void testUrlServiceAddGet() {
    qt()
      .forAll(
        strings().basicLatinAlphabet().ofLengthBetween(4,10),
        strings().basicLatinAlphabet().ofLengthBetween(4,10)
      ).check((alias, url) -> {
      url = url.concat(".com");
      urlService.addUrl(alias, url, "jusovch");
      var gotUrl = urlService.getUrl("jusovch");
      if (gotUrl == null) {
        return false;
      }
      if (!gotUrl.equals(url)) {
        return false;
      }
      urlService.deleteAlias(alias, "jusovch");
      return true;
    });
  }



  @Test
  void testDataServiceAddGet() {
    qt()
      .forAll(
        strings().basicLatinAlphabet().ofLengthBetween(4,10),
        strings().basicLatinAlphabet().ofLengthBetween(4,10)
      ).check((alias, url) -> {
      url = url.concat(".com");
      dataService.addUrlAlias(new UrlAlias(alias, url, "jusovch"));
      var gotUrlAlias = dataService.getUrlAlias(alias);
      if (gotUrlAlias == null) {
        return false;
      }
      if (!gotUrlAlias.getUrl().equals(url)) {
        return false;
        }
      dataService.deleteUrlAlias(alias, "jusovch");
      return true;
    });
  }
}
