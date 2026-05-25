package com.stylish.wardrobe.profile;

import org.mapstruct.Mapper;

import com.stylish.wardrobe.profile.dto.DmitriyProfileResponse;

@Mapper(componentModel = "spring")
public interface DmitriyProfileMapper {
	DmitriyProfileResponse toResponse(DmitriyProfileEntity entity);
}

