package com.hotel.reservations.backends;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherApiResponse {

    private double latitude;
    private double longitude;
    private Daily daily;

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public Daily getDaily() {
        return daily;
    }

    public void setDaily(Daily daily) {
        this.daily = daily;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Daily {

        private List<String> time;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperatureMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperatureMin;

        @JsonProperty("wind_speed_10m_max")
        private List<Double> windSpeedMax;

        @JsonProperty("weather_code")
        private List<Integer> weatherCode;

        public List<String> getTime() {
            return time;
        }

        public void setTime(List<String> time) {
            this.time = time;
        }

        public List<Double> getTemperatureMax() {
            return temperatureMax;
        }

        public void setTemperatureMax(List<Double> temperatureMax) {
            this.temperatureMax = temperatureMax;
        }

        public List<Double> getTemperatureMin() {
            return temperatureMin;
        }

        public void setTemperatureMin(List<Double> temperatureMin) {
            this.temperatureMin = temperatureMin;
        }

        public List<Double> getWindSpeedMax() {
            return windSpeedMax;
        }

        public void setWindSpeedMax(List<Double> windSpeedMax) {
            this.windSpeedMax = windSpeedMax;
        }

        public List<Integer> getWeatherCode() {
            return weatherCode;
        }

        public void setWeatherCode(List<Integer> weatherCode) {
            this.weatherCode = weatherCode;
        }
    }
}
