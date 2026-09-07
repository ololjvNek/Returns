package pl.returns;

import org.springframework.boot.SpringApplication;

public class TestEwidencjaZwrotowApplication {

	public static void main(String[] args) {
		SpringApplication.from(EwidencjaZwrotowApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
