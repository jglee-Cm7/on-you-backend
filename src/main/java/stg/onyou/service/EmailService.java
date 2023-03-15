package stg.onyou.service;

import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import stg.onyou.model.entity.User;
import stg.onyou.repository.UserRepository;

import java.security.SecureRandom;
import java.util.Date;

@Service
@AllArgsConstructor
public class EmailService {

    private JavaMailSender emailSender;
    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private static SimpleMailMessage message = new SimpleMailMessage();
    @Autowired
    private RedisTemplate<String, String> redisTemplate;


    public void sendSimpleMessage(User user) {
        message.setFrom("stg8onyou@gmail.com");
        message.setTo(user.getEmail());
        message.setSubject("[시광교회]Onyou - 임시 비밀번호 발급 안내");
        String tempPw = getRandomPassword(6);
        message.setText("임시 비밀번호는 " + tempPw + "입니다. ");
        emailSender.send(message);
        user.setPassword(passwordEncoder.encode(tempPw));  // set password to encrypted tempPw
        userRepository.save(user);
    }

    public String getRandomPassword(int size) {
        char[] charSet = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
                'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z'
                };

        StringBuffer sb = new StringBuffer();
        SecureRandom sr = new SecureRandom();
        sr.setSeed(new Date().getTime());

        int idx = 0;
        int len = charSet.length;
        for (int i=0; i<size; i++) {
            // idx = (int) (len * Math.random());
            idx = sr.nextInt(len);    // 강력한 난수를 발생시키기 위해 SecureRandom을 사용한다.
            sb.append(charSet[idx]);
        }

        return sb.toString();
    }

    public void sendValidCheckEmail(String email) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("stg8onyou@gmail.com");
        message.setTo(email);
        message.setSubject("[시광교회]Onyou - 인증번호 발급 안내");
        String checkString = getRandomPassword(6);
        message.setText("인증번호는 " + checkString + "입니다. ");

        emailSender.send(message);

        // 메일로 보낸 checkString을 value로, email을 key로 해서 redis캐시에 저장.
        String redisKey = "check:" + email;
        redisTemplate.opsForValue().set(redisKey, checkString);
    }
}
