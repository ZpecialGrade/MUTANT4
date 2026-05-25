package com.stylish.wardrobe.photo;

import org.mapstruct.Mapper;

import com.stylish.wardrobe.photo.dto.DmitriyUserPhotoResponse;

@Mapper(componentModel = "spring")
public interface DmitriyUserPhotoMapper {
	DmitriyUserPhotoResponse toResponse(DmitriyUserPhotoEntity entity);
}

