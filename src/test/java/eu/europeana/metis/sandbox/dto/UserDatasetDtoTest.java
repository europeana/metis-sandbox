package eu.europeana.metis.sandbox.dto;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;

class UserDatasetDtoTest {

  @Test
  void testCreate() {
    UserDatasetDto userDatasetDto = new UserDatasetDto();
    assertNotNull(userDatasetDto);
  }

}
