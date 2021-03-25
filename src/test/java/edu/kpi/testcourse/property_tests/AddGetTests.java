package edu.kpi.testcourse.property_tests;
import edu.kpi.testcourse.dataservice.DataService;
import edu.kpi.testcourse.dataservice.UrlAlias;
import edu.kpi.testcourse.dataservice.User;
import edu.kpi.testcourse.urlservice.AliasInfo;
import edu.kpi.testcourse.urlservice.UrlService;
import static org.quicktheories.generators.SourceDSL.strings;
import io.micronaut.context.BeanContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.quicktheories.QuickTheory.qt;

public class AddGetTests {
  static DataService dataService = BeanContext.run().getBean(DataService.class);
  static UrlService urlService = BeanContext.run().getBean(UrlService.class);

  @BeforeAll
  static void beforeAll() {
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
      var allUserAliases = urlService.getUserAliases("jusovch");
      for (AliasInfo urlAlias : allUserAliases) {
        if (!urlAlias.alias().equals(alias) || !urlAlias.url().equals(url)) {
          return false;
        }
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
      for (UrlAlias urlAlias : dataService.getUserAliases("jusovch")) {
        if (!urlAlias.getAlias().equals(alias) || !urlAlias.getUrl().equals(url)) {
          return false;
        }
      }
      dataService.deleteUrlAlias(alias, "jusovch");
      return true;
    });
  }
}
