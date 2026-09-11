package com.example.ThirdProject.unitTest;

import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.entity.Track;
import com.example.ThirdProject.project.enums.TrackType;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.track.service.TrackService;
import com.example.ThirdProject.user.entity.User;
import com.example.ThirdProject.common.exception.CustomException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import static org.mockito.BDDMockito.given;
import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TrackServiceTest {

    @Mock
    ProjectRepository projectRepository;

    @InjectMocks
    TrackService trackService;

    @Test
    void deleteTrack_shouldSoftDeleteTrack_whenOwnerDeletes() {

        // given
        String userId = "id";
        Long projectId = 1L;
        Long targetTrackId = 2L;

        User user = createUser(userId);
        Project project = createProject(projectId, user);

        Track track1 = createTrack(1L, project);
        Track track2 = createTrack(2L, project);
        Track track3 = createTrack(3L, project);

        project.setTracks(List.of(track1, track2, track3));

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when
        trackService.deleteTrack(projectId, targetTrackId, userId);

        // then
        assertThat(track2.isDeleted()).isTrue();
    }

    @Test
    void deleteTrack_shouldThrowException_whenUserIsNotOwner() {
        // given
        String ownerId = "id";
        String wrongUserId = "anotherUser";
        Long projectId = 1L;
        Long trackId = 1L;

        User owner = createUser(ownerId);
        Project project = createProject(projectId, owner);

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() ->
                trackService.deleteTrack(projectId, trackId, wrongUserId)
        ).isInstanceOf(CustomException.class);
    }

    // ---------------- fixture ----------------

    private User createUser(String id) {
        return new User(
                id,
                "loginId",
                "nickname",
                "password",
                "email",
                "phone",
                "name",
                "intro",
                "profile",
                false,
                LocalDateTime.now()
        );
    }

    private Project createProject(Long id, User user) {
        return new Project(
                id,
                user,
                null,
                "projectName",
                120,
                "4/4",
                null
        );
    }

    private Track createTrack(Long id, Project project) {
        return new Track(
                id,
                project,
                "track",
                TrackType.AUDIO,
                "color",
                0.0,
                0.0,
                false,
                false,
                1,
                false,
                null,
                null
        );
    }
}