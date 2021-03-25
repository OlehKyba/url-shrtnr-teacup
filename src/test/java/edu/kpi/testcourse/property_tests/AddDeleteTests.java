package edu.kpi.testcourse.property_tests;

import edu.kpi.testcourse.dataservice.DataService;
import edu.kpi.testcourse.dataservice.UrlAlias;
import edu.kpi.testcourse.dataservice.User;
import edu.kpi.testcourse.urlservice.UrlService;
import io.micronaut.context.BeanContext;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class AddDeleteTests {

  static DataService dataService = BeanContext.run().getBean(DataService.class);
  static UrlService urlService = BeanContext.run().getBean(UrlService.class);

  @BeforeAll
  static void beforeAll() {
    dataService.clear();
    dataService.addUser(new User("aaa@bbb.com", "aaabbbccc"));
  }

  @Test
  void testUrlServiceAddUrlDeleteAlias_propertyBased(){
    qt()
      .forAll (
        strings().basicLatinAlphabet().ofLengthBetween(1, 29), //alias
        strings().basicLatinAlphabet().ofLengthBetween(1, 40) //url
      ).check((alias, url) -> {
      urlService.addUrl(alias, url, "aaa@bbb.com");
      urlService.deleteAlias(alias, "aaa@bbb.com");
      return urlService.getUrl(alias) == null;
    });
  }

  @Test
  void testDataServiceAddUrlAliasDeleteUrlAlias_propertyBased() {
    qt()
      .forAll (
        strings().basicLatinAlphabet().ofLengthBetween(1, 29), //alias
        strings().basicLatinAlphabet().ofLengthBetween(1, 40) //url
      ).check((alias, url) -> {
      dataService.addUrlAlias(new UrlAlias(alias, url, "aaa@bbb.com"));
      dataService.deleteUrlAlias(alias, "aaa@bbb.com");
      return dataService.getUrlAlias(alias) == null;
    });
  }

}
