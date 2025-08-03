package app.g_agent.policy_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AddressDto {

	private Long id;

	private String address;

	private Long locality;

	private Long town;

	@JsonProperty("administrative_area")
	private Long administrativeArea;

	@JsonProperty("country_code")
	private Long countryCode;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getAddress() {
		return address;
	}

	public void setAddress(String address) {
		this.address = address;
	}

	public Long getLocality() {
		return locality;
	}

	public void setLocality(Long locality) {
		this.locality = locality;
	}

	public Long getTown() {
		return town;
	}

	public void setTown(Long town) {
		this.town = town;
	}

	public Long getAdministrativeArea() {
		return administrativeArea;
	}

	public void setAdministrativeArea(Long administrativeArea) {
		this.administrativeArea = administrativeArea;
	}

	public Long getCountryCode() {
		return countryCode;
	}

	public void setCountryCode(Long countryCode) {
		this.countryCode = countryCode;
	}

}
