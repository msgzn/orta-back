package org.example.ortaback.controller;

import org.example.ortaback.config.GeoProperties;
import org.example.ortaback.dto.geo.BoundingBoxResultDto;
import org.example.ortaback.dto.geo.ClusterDto;
import org.example.ortaback.dto.geo.EventMapMarkerDto;
import org.example.ortaback.dto.geo.geojson.GeoJsonFeatureCollectionDto;
import org.example.ortaback.exception.InvalidCoordinatesException;
import org.example.ortaback.persistence.models.User;
import org.example.ortaback.persistence.models.enums.LocationPrecision;
import org.example.ortaback.persistence.models.enums.UserRole;
import org.example.ortaback.service.EventService;
import org.example.ortaback.testsupport.MockMvcTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class MapControllerTest extends MockMvcTestSupport {

    private final EventService eventService = mock(EventService.class);
    private final GeoProperties geoProperties = new GeoProperties(
            4326, 10, 100, 1000, 50, 200, null, null);

    private MockMvc mvc;
    private final User user = User.builder()
            .id(1L).email("u@example.com")
            .firstName("U").lastName("U")
            .hashedPassword("BCRYPT").role(UserRole.USER)
            .searchRadiusKm(15)
            .build();

    @BeforeEach
    void setUp() {
        mvc = buildMockMvc(new MapController(eventService, geoProperties));
        authenticate(user);
    }

    // -----------------------------------------------------------------------
    //  /nearby
    // -----------------------------------------------------------------------

    @Test
    void nearby_withExplicitRadius_returnsMarkers() throws Exception {
        when(eventService.findNearbyEvents(eq(43.22), eq(76.85), eq(5000.0), any()))
                .thenReturn(List.of(new EventMapMarkerDto(
                        100L, "Match", 43.22, 76.85, null, 3L, LocationPrecision.EXACT)));

        mvc.perform(get("/api/v1/map/events/nearby")
                        .param("lat", "43.22").param("lng", "76.85").param("radius", "5000"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("Match"));
    }

    @Test
    void nearby_withoutRadius_fallsBackToUserPreference() throws Exception {
        // user.searchRadiusKm = 15 → 15_000m
        when(eventService.findNearbyEvents(eq(43.22), eq(76.85), eq(15_000.0), any()))
                .thenReturn(List.of());

        mvc.perform(get("/api/v1/map/events/nearby")
                        .param("lat", "43.22").param("lng", "76.85"))
                .andExpect(status().isOk());
    }

    @Test
    void nearby_serviceRejectsBadCoords_returns400() throws Exception {
        when(eventService.findNearbyEvents(anyDouble(), anyDouble(), anyDouble(), any()))
                .thenThrow(new InvalidCoordinatesException("Latitude out of range"));

        mvc.perform(get("/api/v1/map/events/nearby")
                        .param("lat", "200").param("lng", "76.85").param("radius", "5000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Latitude out of range"));
    }

    // -----------------------------------------------------------------------
    //  /bbox
    // -----------------------------------------------------------------------

    @Test
    void bbox_returnsMarkersResultEnvelope() throws Exception {
        when(eventService.findInBoundingBox(any(), any())).thenReturn(
                BoundingBoxResultDto.markers(2, List.of()));

        mvc.perform(get("/api/v1/map/events/bbox")
                        .param("minLat", "40").param("minLng", "70")
                        .param("maxLat", "50").param("maxLng", "80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("MARKERS"))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void bbox_returnsClustersResultEnvelope() throws Exception {
        when(eventService.findInBoundingBox(any(), any())).thenReturn(
                BoundingBoxResultDto.clusters(5000, List.of(
                        new ClusterDto(0, 1500L, 45.0, 75.0))));

        mvc.perform(get("/api/v1/map/events/bbox")
                        .param("minLat", "40").param("minLng", "70")
                        .param("maxLat", "50").param("maxLng", "80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CLUSTERS"))
                .andExpect(jsonPath("$.clusters[0].eventCount").value(1500));
    }

    @Test
    void bbox_invertedLatitudes_returns400() throws Exception {
        // BoundingBox.of() кинет InvalidCoordinatesException ещё на парсинге
        mvc.perform(get("/api/v1/map/events/bbox")
                        .param("minLat", "60").param("minLng", "70")
                        .param("maxLat", "50").param("maxLng", "80"))
                .andExpect(status().isBadRequest());
    }

    // -----------------------------------------------------------------------
    //  /clusters
    // -----------------------------------------------------------------------

    @Test
    void clusters_returnsClusterList() throws Exception {
        when(eventService.findClustersInBoundingBox(any(), any())).thenReturn(List.of(
                new ClusterDto(0, 50L, 43.0, 76.0),
                new ClusterDto(1, 30L, 43.5, 76.5)));

        mvc.perform(get("/api/v1/map/events/clusters")
                        .param("minLat", "40").param("minLng", "70")
                        .param("maxLat", "50").param("maxLng", "80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$[0].clusterId").value(0));
    }

    // -----------------------------------------------------------------------
    //  /geojson
    // -----------------------------------------------------------------------

    @Test
    void geoJson_returnsFeatureCollection() throws Exception {
        when(eventService.findGeoJsonInBoundingBox(any(), any()))
                .thenReturn(GeoJsonFeatureCollectionDto.of(List.of()));

        mvc.perform(get("/api/v1/map/events/geojson")
                        .param("minLat", "40").param("minLng", "70")
                        .param("maxLat", "50").param("maxLng", "80"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("FeatureCollection"));
    }
}
