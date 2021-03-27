package edu.kpi.testcourse.property_tests;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import javax.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.quicktheories.core.Gen;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;
import edu.kpi.testcourse.dataservice.DataService;
import edu.kpi.testcourse.dataservice.UrlAlias;
import edu.kpi.testcourse.dataservice.User;
import edu.kpi.testcourse.urlservice.AliasInfo;
import edu.kpi.testcourse.urlservice.UrlService;
import org.junit.jupiter.api.Test;

@MicronautTest
public class AddUrlGetAllUserAliasesTest {

  @Inject DataService dataService;
  @Inject UrlService urlService;

  static Gen<String> aliases = strings().basicLatinAlphabet().ofLengthBetween(1, 10);
  static Gen<String> urls = strings().basicLatinAlphabet().ofLengthBetween(1, 10);

  @BeforeEach
  void prepare() {
    dataService.clear();
    dataService.addUser(new User("vasya", "pupkin"));
  }

  @Test
  void testUrlServiceAddGetAll() {
    qt()
      .forAll(
        aliases,
        urls
      ).check((alias, url) -> {
        url = url.concat(".com");
        assert(urlService.addUrl(alias, url, "vasya"));
        var allUserAliases = urlService.getUserAliases("vasya");
        assert(allUserAliases.size() != 0);
        for (AliasInfo urlAlias : allUserAliases) {
          if (!urlAlias.alias().equals(alias) || !urlAlias.url().equals(url)) {
            return false;
          }
        }
        return true;
    });
  }

  @Test
  void testDataServiceAddGetAll() {
    qt()
      .forAll(
        aliases,
        urls
      ).check((alias, url) -> {
        url = url.concat(".com");
        assert(dataService.addUrlAlias(new UrlAlias(alias, url, "vasya")));
        var allUserAliases = dataService.getUserAliases("vasya");
        assert(allUserAliases.size() != 0);
        for (UrlAlias urlAlias : allUserAliases) {
          if (!urlAlias.getAlias().equals(alias) || !urlAlias.getUrl().equals(url)) {
            return false;
          }
        }
        return true;
      });
  }
}
