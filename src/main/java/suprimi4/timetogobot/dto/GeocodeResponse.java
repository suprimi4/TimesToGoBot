package suprimi4.timetogobot.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GeocodeResponse {
    @JsonProperty("result")
    public String result;

    @JsonProperty("geo_lat")
    private Double latitude;

    @JsonProperty("geo_lon")
    private Double longitude;

    @JsonProperty("timezone")
    private String timezone;

}
