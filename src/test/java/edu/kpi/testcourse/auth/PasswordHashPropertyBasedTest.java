package edu.kpi.testcourse.auth;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import org.junit.jupiter.api.Test;
import static org.quicktheories.QuickTheory.qt;
import static org.quicktheories.generators.SourceDSL.strings;

public class PasswordHashPropertyBasedTest {

  @Test
  void shouldCreateValidPasswordHash_propertyBase() {
    qt()
      .forAll(
        // GIVEN
        strings()
          .allPossible()
          .ofLengthBetween(5, 20)
      )
      .check(password -> {
        // WHEN + THEN
        try {
          return PasswordHash.validatePassword(
            password, PasswordHash.createHash(password));
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
          return false;
        }
      });
  }

}
