package edu.kpi.testcourse.property_tests;

import edu.kpi.testcourse.dataservice.DataService;
import edu.kpi.testcourse.dataservice.UrlAlias;
import edu.kpi.testcourse.dataservice.User;
import edu.kpi.testcourse.urlservice.UrlService;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

@MicronautTest
public class AddDeleteTests {

  @Inject DataService dataService;
  @Inject UrlService urlService;

  //There are some troubles with addUrl method in both interfaces
  //addUrl
  @Test
  void testUrlServiceAddUrlDeleteAlias_propertyBased(){
    dataService.clear();
    dataService.addUser(new User("aaa@bbb.com", "aaabbbccc"));
    qt()
      .forAll (
        strings().basicLatinAlphabet().ofLengthBetween(1, 29), //alias
        strings().basicLatinAlphabet().ofLengthBetween(1, 40) //url
      ).check((alias, url) -> {
        if (!urlService.addUrl(alias, url, "aaa@bbb.com")) {
          return false;
        }
      urlService.deleteAlias(alias, "aaa@bbb.com");
      return urlService.getUrl(alias) == null;
    });
  }

  @Test
  void testDataServiceAddUrlAliasDeleteUrlAlias_propertyBased() {
    dataService.clear();
    dataService.addUser(new User("aaa@bbb.com", "aaabbbccc"));
    qt()
      .forAll (
        strings().basicLatinAlphabet().ofLengthBetween(1, 29), //alias
        strings().basicLatinAlphabet().ofLengthBetween(1, 40) //url
      ).check((alias, url) -> {
      assert(dataService.addUrlAlias(new UrlAlias(alias, url, "aaa@bbb.com")));
      dataService.deleteUrlAlias(alias, "aaa@bbb.com");
      return dataService.getUrlAlias(alias) == null;
    });
  }

}

