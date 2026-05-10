package com.afternote.feature.afternote.domain.usecase.editor

import com.afternote.feature.afternote.domain.model.author.CreateAfternoteInput
import com.afternote.feature.afternote.domain.model.author.SaveAfternoteCommand
import com.afternote.feature.afternote.domain.repository.author.AfternoteRepository
import javax.inject.Inject

class SaveAfternoteUseCase
    @Inject
    constructor(
        private val afternoteRepository: AfternoteRepository,
    ) {
        suspend operator fun invoke(command: SaveAfternoteCommand): Result<Long> =
            when (command) {
                is SaveAfternoteCommand.Create ->
                    when (val input = command.input) {
                        is CreateAfternoteInput.Social -> afternoteRepository.createSocial(input.payload)
                        is CreateAfternoteInput.Gallery -> afternoteRepository.createGallery(input.payload)
                        is CreateAfternoteInput.Playlist -> afternoteRepository.createPlaylist(input.payload)
                    }

                is SaveAfternoteCommand.Update -> afternoteRepository.update(command.id, command.payload)
            }
    }
