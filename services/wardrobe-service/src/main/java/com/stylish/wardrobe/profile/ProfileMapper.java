package com.stylish.wardrobe.profile;

import org.mapstruct.Mapper;

import com.stylish.wardrobe.profile.dto.ProfileResponse;

@Mapper(componentModel = "spring")
public interface ProfileMapper {
	ProfileResponse toResponse(ProfileEntity entity);
}

