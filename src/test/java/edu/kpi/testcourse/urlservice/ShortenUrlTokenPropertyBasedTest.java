package edu.kpi.testcourse.urlservice;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import javax.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.integers;

@MicronautTest
public class ShortenUrlTokenPropertyBasedTest {

  @Inject
  UrlService urlService;

  @Test
  void shouldGenerateValidUrlToken_propertyBased() {
    qt()
      .forAll(
        // GIVEN
        integers().allPositive()
      )
      .check(urlId -> {
        // WHEN
        String urlToken = urlService.shortenUrlToken(urlId);
        // THEN
        return urlService.isUserAliasValid(urlToken);
      });
  }

}
