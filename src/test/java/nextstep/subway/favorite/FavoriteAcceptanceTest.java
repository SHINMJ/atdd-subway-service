package nextstep.subway.favorite;

import io.restassured.RestAssured;
import io.restassured.response.ExtractableResponse;
import io.restassured.response.Response;
import nextstep.subway.AcceptanceTest;
import nextstep.subway.auth.acceptance.AuthAcceptanceTest;
import nextstep.subway.favorite.dto.FavoriteRequest;
import nextstep.subway.favorite.dto.FavoriteResponse;
import nextstep.subway.line.dto.LineRequest;
import nextstep.subway.line.dto.LineResponse;
import nextstep.subway.member.MemberAcceptanceTest;
import nextstep.subway.station.StationAcceptanceTest;
import nextstep.subway.station.dto.StationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Arrays;
import java.util.List;

import static nextstep.subway.line.acceptance.LineAcceptanceTest.지하철_노선_등록되어_있음;
import static nextstep.subway.line.acceptance.LineSectionAcceptanceTest.지하철_노선에_지하철역_등록_요청;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("즐겨찾기 관련 기능")
public class FavoriteAcceptanceTest extends AcceptanceTest {

    private LineResponse 신분당선;
    private LineResponse 이호선;
    private LineResponse 삼호선;
    private StationResponse 강남역;
    private StationResponse 양재역;
    private StationResponse 교대역;
    private StationResponse 남부터미널역;
    private String accessToken;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        강남역 = StationAcceptanceTest.지하철역_등록되어_있음("강남역").as(StationResponse.class);
        양재역 = StationAcceptanceTest.지하철역_등록되어_있음("양재역").as(StationResponse.class);
        교대역 = StationAcceptanceTest.지하철역_등록되어_있음("교대역").as(StationResponse.class);
        남부터미널역 = StationAcceptanceTest.지하철역_등록되어_있음("남부터미널역").as(StationResponse.class);

        신분당선 = 지하철_노선_등록되어_있음(
                new LineRequest("신분당선", "bg-red-600", 강남역.getId(), 양재역.getId(), 10)
        ).as(LineResponse.class);

        이호선 = 지하철_노선_등록되어_있음(
                new LineRequest("이호선", "bg-red-600", 교대역.getId(), 강남역.getId(), 10)
        ).as(LineResponse.class);

        삼호선 = 지하철_노선_등록되어_있음(
                new LineRequest("삼호선", "bg-red-600", 교대역.getId(), 양재역.getId(), 5)
        ).as(LineResponse.class);

        지하철_노선에_지하철역_등록_요청(삼호선, 교대역, 남부터미널역, 3);

        MemberAcceptanceTest.회원_생성을_요청(MemberAcceptanceTest.EMAIL, MemberAcceptanceTest.PASSWORD, MemberAcceptanceTest.AGE);

        ExtractableResponse<Response> authResponse =
                AuthAcceptanceTest.bearer_토큰_인증_요청(MemberAcceptanceTest.EMAIL, MemberAcceptanceTest.PASSWORD);
        accessToken = AuthAcceptanceTest.getAccessToken(authResponse);
    }

    @DisplayName("즐겨찾기를 관리한다.")
    @Test
    void manageFavorite() {
        ExtractableResponse<Response> createResponse = 즐겨찾기_생성_요청(
                accessToken, new FavoriteRequest(강남역.getId(), 양재역.getId())
        );
        즐겨찾기_생성됨(createResponse);
        createResponse = 즐겨찾기_생성_요청(accessToken, new FavoriteRequest(강남역.getId(), 교대역.getId()));
        즐겨찾기_생성됨(createResponse);

        ExtractableResponse<Response> findResponse = 즐겨찾기_조회_요청(accessToken);
        즐겨찾기_조회됨(findResponse, 2);
        List<FavoriteResponse> findFavorites = 즐겨찾기조회_결과_추출(findResponse);

        ExtractableResponse<Response> deleteResponse = 즐겨찾기_삭제_요청(accessToken, findFavorites.get(1).getId());
        즐겨찾기_삭제됨(deleteResponse);

        findResponse = 즐겨찾기_조회_요청(accessToken);
        즐겨찾기_조회됨(findResponse, 1);
    }

    public static ExtractableResponse<Response> 즐겨찾기_생성_요청(String accessToken, FavoriteRequest favoriteRequest) {
        return RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .body(favoriteRequest)
                .when().post("/favorites")
                .then().log().all().extract();
    }

    public static ExtractableResponse<Response> 즐겨찾기_조회_요청(String accessToken) {
        return RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .contentType(MediaType.APPLICATION_JSON_VALUE)
                .when().get("/favorites")
                .then().log().all().extract();
    }

    public static ExtractableResponse<Response> 즐겨찾기_삭제_요청(String accessToken, Long favoriteId) {
        return RestAssured
                .given().log().all()
                .auth().oauth2(accessToken)
                .when().delete("/favorites/" + favoriteId)
                .then().log().all()
                .extract();
    }

    public static void 즐겨찾기_생성됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.CREATED.value());
    }

    public static void 즐겨찾기_조회됨(ExtractableResponse<Response> response, int size) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        List<FavoriteResponse> favoriteResponse = 즐겨찾기조회_결과_추출(response);
        assertThat(favoriteResponse.size()).isEqualTo(size);
    }

    public static void 즐겨찾기_삭제됨(ExtractableResponse<Response> response) {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.NO_CONTENT.value());
    }

    public static List<FavoriteResponse> 즐겨찾기조회_결과_추출(ExtractableResponse<Response> findResponse) {
        return Arrays.asList(findResponse.as(FavoriteResponse[].class));
    }
}