package eu.europeana.metis.sandbox.controller;

import static eu.europeana.metis.security.AuthenticationUtils.getUserId;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

import eu.europeana.metis.sandbox.dto.DatasetWithExecutionProgressSummaryDTO;
import eu.europeana.metis.sandbox.service.user.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This class provides API endpoints for accessing user-related information.
 */
@RestController
@RequestMapping("/users/")
@Tag(name = "Users Controller")
public class UserController {

  private final UserService userService;

  /**
   * Constructor.
   *
   * @param userService the {@code UserService} instance used to manage and retrieve user-related information and actions.
   */
  public UserController(UserService userService) {
    this.userService = userService;
  }

  /**
   * GET API call that retrieves the datasets belonging to the current user.
   *
   * @param jwtPrincipal the security principal containing user authentication details.
   *                     This parameter determines the user's identity, and if null,
   *                     datasets cannot be associated with a specific user.
   * @return a list of {@code DatasetWithExecutionProgressInfoDTO} objects representing the datasets
   *         owned by the current user along with their execution progress information.
   */
  @Operation(summary = "Get user's datasets", description = "Get datasets belonging to the current user")
  @ApiResponse(responseCode = "200", description = "Success")
  @ApiResponse(responseCode = "404", description = "User datasets not found")
  @ApiResponse(responseCode = "400", description = "Error")
  @GetMapping(value = "/me/datasets", produces = APPLICATION_JSON_VALUE)
  public List<DatasetWithExecutionProgressSummaryDTO> getUserDatasets(@AuthenticationPrincipal Jwt jwtPrincipal) {
    //Check user id if any. This is temporarily allowed due to api and ui user security.
    final String userId;
    if (jwtPrincipal == null) {
      userId = null;
    } else {
      userId = getUserId(jwtPrincipal);
    }
    return userService.getDatasetInfoWithProgressOwnedByUserId(userId);
  }
}
