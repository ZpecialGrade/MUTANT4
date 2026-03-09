package com.stylish.wardrobe.photo;

import org.mapstruct.Mapper;

import com.stylish.wardrobe.photo.dto.UserPhotoResponse;

@Mapper(componentModel = "spring")
public interface UserPhotoMapper {
	UserPhotoResponse toResponse(UserPhotoEntity entity);
}

