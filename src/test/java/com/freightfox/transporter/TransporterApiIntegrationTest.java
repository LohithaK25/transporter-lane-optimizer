package com.freightfox.transporter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TransporterApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String BASE_INPUT = """
        {
          "lanes": [
            {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
            {"id": 2, "origin": "Delhi", "destination": "Bangalore"}
          ],
          "transporters": [
            {
              "id": 1,
              "name": "Transporter T1",
              "laneQuotes": [
                {"laneId": 1, "quote": 5000},
                {"laneId": 2, "quote": 6000}
              ]
            },
            {
              "id": 2,
              "name": "Transporter T2",
              "laneQuotes": [
                {"laneId": 1, "quote": 4500}
              ]
            }
          ]
        }
        """;

    @BeforeEach
    void loadBaselineData() throws Exception {
        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BASE_INPUT))
                .andExpect(status().isOk());
    }

    @Test
    void submitsInputSuccessfully() throws Exception {

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(BASE_INPUT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message")
                        .value("Input data saved successfully."));
    }

    @Test
    void returnsOptimalAssignment() throws Exception {

        mockMvc.perform(post("/api/v1/transporters/assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"maxTransporters": 2}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.totalCost").value(10500.0))
                .andExpect(jsonPath("$.assignments.length()").value(2))
                .andExpect(jsonPath("$.assignments[0].laneId").value(1))
                .andExpect(jsonPath("$.assignments[0].transporterId").value(2))
                .andExpect(jsonPath("$.assignments[1].laneId").value(2))
                .andExpect(jsonPath("$.assignments[1].transporterId").value(1))
                .andExpect(jsonPath("$.selectedTransporters.length()").value(2));
    }

    @Test
    void rejectsUnknownLaneReferenceAndPreservesExistingData()
            throws Exception {

        String invalidInput = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"}
              ],
              "transporters": [
                {
                  "id": 1,
                  "name": "Transporter T1",
                  "laneQuotes": [
                    {"laneId": 99, "quote": 5000}
                  ]
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidInput))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("unknown lane ID: 99")));

        // The previously persisted dataset must remain available.
        mockMvc.perform(post("/api/v1/transporters/assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"maxTransporters": 2}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(10500.0));
    }

    @Test
    void rejectsZeroTransporterLimit() throws Exception {

        mockMvc.perform(post("/api/v1/transporters/assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"maxTransporters": 0}
                    """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));
    }

    @Test
    void returnsUnprocessableEntityWhenALaneHasNoQuotes()
            throws Exception {

        String inputWithoutLaneTwoQuote = """
            {
              "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 2, "origin": "Delhi", "destination": "Bangalore"}
              ],
              "transporters": [
                {
                  "id": 1,
                  "name": "Transporter T1",
                  "laneQuotes": [
                    {"laneId": 1, "quote": 5000}
                  ]
                }
              ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(inputWithoutLaneTwoQuote))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"maxTransporters": 1}
                    """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message")
                        .value(containsString(
                                "No transporter quote available for lane 2"
                        )));
    }

    @Test
    void rejectsDuplicateLaneIds() throws Exception {

        String body = """
            {
            "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"},
                {"id": 1, "origin": "Pune", "destination": "Hyderabad"}
            ],
            "transporters": [
                {
                "id": 1,
                "name": "T1",
                "laneQuotes": [
                    {"laneId": 1, "quote": 5000}
                ]
                }
            ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("Duplicate lane ID: 1")));
    }

    @Test
    void rejectsDuplicateTransporterIds() throws Exception {

        String body = """
            {
            "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"}
            ],
            "transporters": [
                {"id": 1, "name": "T1", "laneQuotes": []},
                {"id": 1, "name": "T2", "laneQuotes": []}
            ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("Duplicate transporter ID: 1")));
    }

    @Test
    void rejectsDuplicateQuotesForSameLaneAndTransporter()
            throws Exception {

        String body = """
            {
            "lanes": [
                {"id": 1, "origin": "Mumbai", "destination": "Delhi"}
            ],
            "transporters": [
                {
                "id": 1,
                "name": "T1",
                "laneQuotes": [
                    {"laneId": 1, "quote": 5000},
                    {"laneId": 1, "quote": 6000}
                ]
                }
            ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message")
                        .value(containsString("Duplicate quote for lane 1")));
    }

    @Test
    void successfullyReplacesPreviouslyStoredDataset()
            throws Exception {

        String replacement = """
            {
            "lanes": [
                {"id": 10, "origin": "Chennai", "destination": "Hyderabad"}
            ],
            "transporters": [
                {
                "id": 8,
                "name": "Replacement T8",
                "laneQuotes": [
                    {"laneId": 10, "quote": 700}
                ]
                }
            ]
            }
            """;

        mockMvc.perform(post("/api/v1/transporters/input")
                .contentType(MediaType.APPLICATION_JSON)
                .content(replacement))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/transporters/assignment")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"maxTransporters": 1}
                    """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCost").value(700.0))
                .andExpect(jsonPath("$.assignments.length()").value(1))
                .andExpect(jsonPath("$.assignments[0].laneId").value(10))
                .andExpect(jsonPath("$.selectedTransporters[0]").value(8));
    }
}