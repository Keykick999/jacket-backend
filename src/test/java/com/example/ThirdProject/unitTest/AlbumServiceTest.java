package com.example.ThirdProject.unitTest;

import com.example.ThirdProject.album.dto.AlbumCreateRequest;
import com.example.ThirdProject.album.dto.AlbumResponse;
import com.example.ThirdProject.album.entity.Album;
import com.example.ThirdProject.album.repository.AlbumRepository;
import com.example.ThirdProject.album.service.AlbumService;
import com.example.ThirdProject.common.exception.CustomException;
import com.example.ThirdProject.common.exception.ErrorCode;
import com.example.ThirdProject.project.entity.Project;
import com.example.ThirdProject.project.repository.ProjectRepository;
import com.example.ThirdProject.user.entity.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class AlbumServiceTest {

    @Mock
    private AlbumRepository albumRepository;

    @Mock
    private ProjectRepository projectRepository;

    @InjectMocks
    private AlbumService albumService;

    @Test
    public void addProjectToAlbum_fail_when_album_is_not_found() {
        // given
        Long albumId = 1L;
        String userId = "userId";
        User user = createUser(userId);
        Album album = createAlbum(albumId, user);

        given(albumRepository.findById(albumId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                albumService.addProjectToAlbum(userId, albumId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ALBUM_NOT_FOUND);
    }

    @Test
    public void addProjectToAlbum_fail_when_user_is_not_owner_of_album() {
        // given
        Long albumId = 1L;
        String userId = "userId";
        String wrongUserId = "wrongUserId";
        User user = createUser(userId);
        Album album = createAlbum(albumId, user);

        given(albumRepository.findById(albumId))
                .willReturn(Optional.of(album));

        // when & then
        assertThatThrownBy(() ->
                albumService.addProjectToAlbum(wrongUserId, albumId, 1L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    public void addProjectToAlbum_fail_when_project_is_not_found() {
        // given
        Long albumId = 1L;
        Long projectId = 1L;
        String userId = "userId";

        User user = createUser(userId);
        Album album = createAlbum(albumId, user);

        given(albumRepository.findById(albumId))
                .willReturn(Optional.of(album));

        given(projectRepository.findById(projectId))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() ->
                albumService.addProjectToAlbum(userId, albumId, projectId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PROJECT_NOT_FOUND);
    }

    @Test
    public void addProjectToAlbum_fail_when_user_is_not_owner_of_project() {
        // given
        Long albumId = 1L;
        Long projectId = 1L;
        String userId = "userId";

        User albumOwner = createUser(userId);
        User projectOwner = createUser("otherUser");

        Album album = createAlbum(albumId, albumOwner);

        Project project = new Project();
        project.setId(projectId);
        project.setUser(projectOwner);

        given(albumRepository.findById(albumId))
                .willReturn(Optional.of(album));

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when & then
        assertThatThrownBy(() ->
                albumService.addProjectToAlbum(userId, albumId, projectId))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.ACCESS_DENIED);
    }

    @Test
    public void addProjectToAlbum_success() {
        // given
        Long albumId = 1L;
        Long projectId = 1L;
        String userId = "userId";

        User user = createUser(userId);
        Album album = createAlbum(albumId, user);

        Project project = new Project();
        project.setId(projectId);
        project.setUser(user);

        given(albumRepository.findById(albumId))
                .willReturn(Optional.of(album));

        given(projectRepository.findById(projectId))
                .willReturn(Optional.of(project));

        // when
        albumService.addProjectToAlbum(userId, albumId, projectId);

        // then
        assertThat(project.getAlbum()).isEqualTo(album);
    }

    @Test
    void createAlbum_success() {
        // given
        User user = new User();
        user.setId("userId");

        AlbumCreateRequest request =
                new AlbumCreateRequest("title", "coverImageUrl");

        Album savedAlbum = new Album();
        savedAlbum.setTitle("title");
        savedAlbum.setCoverImageUrl("coverImageUrl");
        savedAlbum.setUser(user);

        given(albumRepository.save(any(Album.class)))
                .willReturn(savedAlbum);

        // when
        AlbumResponse response =
                albumService.createAlbum(user, request);

        // then
        assertThat(response.getTitle()).isEqualTo("title");
        assertThat(response.getCoverImageUrl()).isEqualTo("coverImageUrl");

        verify(albumRepository).save(any(Album.class));
    }

    private Album createAlbum(Long albumId, User user) {
        return new Album(albumId, "title", "coverImageUrl", user, null);
    }

    private User createUser(String userId) {
        User user = new User();
        user.setId(userId);
        return user;
    }
}
