package dev.avatar.middle.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.avatar.middle.client.dto.Data;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;

/**
 * Error handler assigned to all REST call to detect and report API call errors.
 *
 * @author Christian Tzolov
 */
public class GlobalResponseErrorHandler implements ResponseErrorHandler {

	@Override
	public boolean hasError(ClientHttpResponse response) throws IOException {
		return response.getStatusCode().isError();
	}

	@Override
	public void handleError(ClientHttpResponse response) throws IOException {
		if (response.getStatusCode().isError()) {
			throw new RuntimeException(String.format("%s - %s", response.getStatusCode().value(), //todo нормальная обработка ошибки и человеческий вывод ошибки клиенту (используя ResponseService)
					new ObjectMapper().readValue(response.getBody(), Data.ResponseError.class)));
		}
	}
}
