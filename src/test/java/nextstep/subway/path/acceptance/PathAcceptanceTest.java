package nextstep.subway.path.acceptance;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.acceptance.AuthAcceptanceTest;
import nextstep.subway.auth.dto.TokenResponse;
import nextstep.subway.line.acceptance.LineAcceptanceTest;
import nextstep.subway.line.acceptance.LineSectionAcceptanceTest;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.member.MemberAcceptanceTest;
import nextstep.subway.path.dto.PathResponse;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

@DisplayName("지하철 경로 조회")
public class PathAcceptanceTest extends AcceptanceTest {

	/**
	 * 교대역    --- *2호선* ---   강남역
	 * |                        |
	 * *3호선*                   *신분당선*
	 * |                        |
	 * 남부터미널역  --- *3호선* ---   양재
	 */
	@DisplayName("최단 경로 조회 > 로그인 사용자")
	@Test
	void 최단_경로_조회_로그인() {
		MemberAcceptanceTest.회원_생성되어_있음("gt@gt.com", "gt", 13);
		String accessToken = AuthAcceptanceTest.토큰_발행("gt@gt.com", "gt").getAccessToken();

		ExtractableResponse<Response> response = 최단_경로_조회(accessToken);

		int 거리별_추가_요금 = 1_450;
		int 노선별_추가_요금 = 900;
		int 나이에_따른_요금 = (int)((거리별_추가_요금 - 350) * 0.8);	//age:13

		요금_검증(response, 나이에_따른_요금 + 노선별_추가_요금);
	}

	@DisplayName("최단 경로 조회 > 비로그인 사용자")
	@Test
	void 최단_경로_조회_비로그인() {
		String accessToken = "no!";

		ExtractableResponse<Response> response = 최단_경로_조회(accessToken);

		int 거리별_추가_요금 = 1_450;
		int 노선별_추가_요금 = 900;
		int 나이에_따른_요금 = 거리별_추가_요금;

		요금_검증(response, 나이에_따른_요금 + 노선별_추가_요금);
	}

	private ExtractableResponse<Response> 최단_경로_조회(final String accessToken) {
		//given
		StationResponse 강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
		StationResponse 양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
		StationResponse 교대역 = StationAcceptanceTest.지하철역_등록되어_있음("교대역").as(StationResponse.class);
		StationResponse 남부터미널역 = StationAcceptanceTest.지하철역_등록되어_있음("남부터미널역").as(StationResponse.class);

		LineResponse 신분당선 = LineAcceptanceTest.지하철_노선_등록되어_있음(new LineRequest("신분당선", "bg-red-600",500, 강남역.getId(), 양재역.getId(), 10)).as(LineResponse.class);
		LineResponse 이호선 = LineAcceptanceTest.지하철_노선_등록되어_있음(new LineRequest("이호선", "bg-red-600", 900, 교대역.getId(), 강남역.getId(), 10)).as(LineResponse.class);
		LineResponse 삼호선 = LineAcceptanceTest.지하철_노선_등록되어_있음(new LineRequest("삼호선", "bg-red-600", 100, 교대역.getId(), 양재역.getId(), 25)).as(LineResponse.class);

		LineSectionAcceptanceTest.지하철_노선에_지하철역_등록되어_있음(삼호선, 교대역, 남부터미널역, 3);

		// when
		ExtractableResponse<Response> response = 최단_경로_조회_요청(accessToken, 양재역, 교대역);

		// then
		최단_경로_조회_응답됨(response);
		최단_경로_조회_검증_역목록(response);
		거리_검증(response);

		return response;
	}

	private void 최단_경로_조회_응답됨(ExtractableResponse<Response> response) {
		assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
	}

	private void 요금_검증(ExtractableResponse<Response> response, int 총_요금) {
		int fare = response.jsonPath().getInt("fare");
		assertThat(fare).isEqualTo(총_요금);
	}

	private void 거리_검증(ExtractableResponse<Response> response) {
		int distance = response.jsonPath().getInt("distance");
		assertThat(distance).isEqualTo(20);
	}

	private void 최단_경로_조회_검증_역목록(ExtractableResponse<Response> response) {
		List<StationResponse> stationResponses = response.jsonPath().getList("stations", StationResponse.class);
		assertThat(stationResponses).extracting("name").containsExactly("교대역", "강남역", "양재역");
	}

	private ExtractableResponse<Response> 최단_경로_조회_요청(String accessToken, StationResponse 양재역, StationResponse 교대역) {
		return RestAssured
			.given().log().all()
			.auth().oauth2(accessToken)
			.when().get("/paths?source={source}&target={target}", 교대역.getId(), 양재역.getId())
			.then().log().all().extract();
	}

}
