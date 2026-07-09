package io.github.ifamneme.mneme_core.infrastructure.web;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.github.ifamneme.mneme_core.domain.UserId;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/v1/me")
public class MeController implements MeApi {

	@Override
	public MeResponseDto getMe(HttpServletRequest request) {
		Object attribute = request.getAttribute(AuthenticationFilter.USER_ID_ATTRIBUTE);
		if (!(attribute instanceof UserId userId)) {
			throw new IllegalStateException("No authenticated UserId found on the request");
		}
		return new MeResponseDto(userId.value());
	}

}
