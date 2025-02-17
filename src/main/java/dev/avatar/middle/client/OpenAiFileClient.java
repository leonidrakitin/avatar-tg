package dev.avatar.middle.client;

import dev.avatar.middle.client.dto.Data;
import dev.avatar.middle.client.dto.Data.DataList;
import dev.avatar.middle.client.dto.Data.File;
import dev.avatar.middle.handler.GlobalResponseErrorHandler;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestClient;

import java.util.function.Consumer;

public class OpenAiFileClient {

	public static final String DEFAULT_BASE_URL = "https://api.openai.com";

	private final RestClient rest;
	private final Consumer<HttpHeaders> multipartContentHeaders;

	private final String openAiToken;
	private final ResponseErrorHandler responseErrorHandler;

	/**
	 * Create new FileApi instance.
	 * @param openAiToken Your OpenAPI api-key.
	 */
	public OpenAiFileClient(String openAiToken) {
		this(DEFAULT_BASE_URL, openAiToken, RestClient.builder());
	}

	/**
	 * Create new FileApi instance.
	 * @param baseUrl the api base url.
	 * @param openAiToken Your OpenAPI api-key.
	 * @param restClientBuilder the {@link RestClient.Builder} to use.
	 */
	public OpenAiFileClient(String baseUrl, String openAiToken, RestClient.Builder restClientBuilder) {
		this.openAiToken = openAiToken;

		this.multipartContentHeaders = headers -> {
			headers.setBearerAuth(openAiToken);
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
		};

		this.responseErrorHandler = new GlobalResponseErrorHandler();

		this.rest = restClientBuilder
				.baseUrl(baseUrl)
				.defaultHeaders(headers -> {
					headers.setBearerAuth(openAiToken);
					headers.setContentType(MediaType.APPLICATION_JSON);
				})
				.build();
	}

	/**
	 * Returns a list of files that belong to the given purpose.
	 *
	 * @param purpose Only return files with the given purpose.
	 * @return Returns a list of {@link File}s object
	 */
	public DataList<File> listFiles(File.Purpose purpose) {

		return this.rest.get()
				.uri("/v1/files?purpose={purpose}", purpose.getText())
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(new ParameterizedTypeReference<>() {
				});
	}

	/**
	 * Upload a file that can be used across various endpoints. The size of all the files uploaded by one organization
	 * can be up to 100 GB.
	 *
	 * The size of individual files can be a maximum of 512 MB. See the Assistants Tools guide to learn more about the
	 * types of files supported. The Fine-tuning API only supports .jsonl files.
	 *
	 * @param file The File object (not file name) to be uploaded.
	 * @param purpose The intended purpose of the uploaded file. Use "fine-tune" for Fine-tuning and "assistants" for
	 * Assistants and Messages.
	 *
	 * @return The uploaded {@link File} object.
	 */
	public File uploadFile(Resource file, File.Purpose purpose) {

		MultiValueMap<String, Object> multipartBody = new LinkedMultiValueMap<>();
		multipartBody.add("purpose", purpose.getText());
		multipartBody.add("file", file);

		return this.rest.post()
				.uri("/v1/files")
				.headers(this.multipartContentHeaders)
				.body(multipartBody)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(File.class);
	}

	/**
	 * Deletes a file by ID.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Return the file deletion status.
	 */
	public Data.DeletionStatus deleteFile(String fileId) {
		return this.rest.delete()
				.uri("/v1/files/{file_id}", fileId)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.DeletionStatus.class);
	}

	/**
	 * Retrieve the {@link Data.File} object matching the specified ID.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Return the {@link Data.File}.
	 */
	public Data.File retrieveFile(String fileId) {
		return this.rest.get()
				.uri("/v1/files/{file_id}", fileId)
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(Data.File.class);
	}

	/**
	 * Returns the contents of the specified file.
	 *
	 * @param fileId The ID of the file to use for this request.
	 * @return Returns the byte array content of the file.
	 */
	public byte[] retrieveFileContent(String fileId) {
		return this.rest.get()
				.uri("/v1/files/{file_id}/content", fileId)
				.headers(headers -> {
					headers.setBearerAuth(this.openAiToken);
				})
				.retrieve()
				.onStatus(this.responseErrorHandler)
				.body(byte[].class);
	}

	/**
	 * Exception throw if FileApi error is detected.
	 */
	public static class FileApiException extends RuntimeException {
		public FileApiException(String message) {
			super(message);
		};
	}
}
