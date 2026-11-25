package com.example.uniride.data.mapper

import com.example.uniride.data.local.entity.CampusEntity
import com.example.uniride.domain.model.Campus
import com.example.uniride.domain.model.CampusCode

fun CampusEntity.toDomain(): Campus = Campus(CampusCode(code), name, meetingPoints)

fun Campus.toEntity(): CampusEntity = CampusEntity(code.value, name, meetingPoints)
