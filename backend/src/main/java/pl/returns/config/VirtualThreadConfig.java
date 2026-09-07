package pl.returns.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.support.TaskExecutorAdapter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.SimpleAsyncTaskScheduler;

@Configuration
public class VirtualThreadConfig {

	@Bean(destroyMethod = "close")
	ExecutorService virtualThreadExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	AsyncTaskExecutor applicationTaskExecutor(ExecutorService virtualThreadExecutor) {
		return new TaskExecutorAdapter(virtualThreadExecutor);
	}

	@Bean
	TaskScheduler taskScheduler() {
		var scheduler = new SimpleAsyncTaskScheduler();
		scheduler.setVirtualThreads(true);
		scheduler.setThreadNamePrefix("harmonogram-");
		return scheduler;
	}
}
