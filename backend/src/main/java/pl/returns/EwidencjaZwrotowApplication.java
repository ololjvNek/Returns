package pl.returns;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class EwidencjaZwrotowApplication {

	public static void main(String[] args) {
		SpringApplication.run(EwidencjaZwrotowApplication.class, args);
	}
}
