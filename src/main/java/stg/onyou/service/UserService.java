package stg.onyou.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import stg.onyou.exception.CustomException;
import stg.onyou.exception.ErrorCode;
import stg.onyou.model.InterestCategory;
import stg.onyou.model.entity.*;
import stg.onyou.model.network.Header;
import stg.onyou.model.network.request.UserCreateRequest;
import stg.onyou.model.network.response.ClubResponse;
import stg.onyou.model.network.response.UserClubResponse;
import stg.onyou.model.network.response.UserResponse;
import stg.onyou.model.network.response.UserUpdateRequest;
import stg.onyou.repository.ClubRepository;
import stg.onyou.repository.OrganizationRepository;
import stg.onyou.repository.UserClubRepository;
import stg.onyou.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private OrganizationRepository organizationRepository;
    @Autowired
    private UserClubRepository userClubRepository;
    @Autowired
    private ClubRepository clubRepository;
    @Autowired
    private AwsS3Service awsS3Service;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public Header<UserResponse> selectUser(Long id){

        return userRepository.findById(id)
                .map(user -> Header.OK(selectUserResponse(user)))
                .orElseThrow(
                        ()-> new CustomException(ErrorCode.USER_NOT_FOUND)
                );
    }

    private UserResponse selectUserResponse(User user){
        List<InterestCategory> interests = user.getInterests().stream().map(Interest::getCategory).collect(Collectors.toList());
        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .organizationName(user.getOrganization().getName())
                .birthday(user.getBirthday())
                .sex(user.getSex())
                .email(user.getEmail())
                .created(user.getCreated())
                .thumbnail(user.getThumbnail())
                .phoneNumber(user.getPhoneNumber())
                .interests(interests)
                .build();

        return userResponse;

    }

    public Header<UserClubResponse> selectUserClubResponse(Long userId, Long clubId) {

        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        Club club = clubRepository.findById(clubId)
                .orElseThrow(
                        () -> new CustomException(ErrorCode.CLUB_NOT_FOUND)
                );

        UserClub userClub = userClubRepository.findByUserAndClub(user, club)
                .orElseThrow(
                        () -> new CustomException(ErrorCode.USER_CLUB_NOT_FOUND)
                );

        UserClubResponse userClubResponse = UserClubResponse.builder()
                .userId(userClub.getUser().getId())
                .clubId(userClub.getClub().getId())
                .applyStatus(userClub.getApplyStatus())
                .approveDate(userClub.getApproveDate())
                .applyDate(userClub.getApplyDate())
                .role(userClub.getRole())
                .build();

        return Header.OK(userClubResponse);
    }

    public Optional<User> findUser(Long id) {
        return userRepository.findById(id);
    }

    public void updateUser(MultipartFile thumbnailFile, UserUpdateRequest userUpdateRequest, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(
                        () -> new CustomException(ErrorCode.USER_NOT_FOUND)
                );

        user.setName(userUpdateRequest.getName());
        user.setBirthday(userUpdateRequest.getBirthday());
        String url = awsS3Service.uploadFile(thumbnailFile);
        user.setThumbnail(url);
        user.setUpdated(LocalDateTime.now());

        Header.OK(userRepository.save(user));
    }

    public Header<User> registerUserInfo(UserCreateRequest userCreateRequest) {

        User user = new User(userCreateRequest);
        Organization organization = organizationRepository.findByName(userCreateRequest.getOrganizationName());
        user.setOrganization(organization);
        user.setPassword(passwordEncoder.encode(userCreateRequest.getPassword()));
        user.setCreated(LocalDateTime.now());
        user.setSocialId("-1");
        return Header.OK(userRepository.save(user));
    }

    public String getUserEmailByNameAndPhoneNumber(String username, String phoneNumber){
        User user = userRepository.findByNameAndPhoneNumber(username, phoneNumber).orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        return user.getEmail();
    }
}