import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RequestService {
    public static void requestService(String[] args) {
        SpringApplication.run(RequestService.class, args);
    }
}
