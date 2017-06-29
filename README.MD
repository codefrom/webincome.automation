# About
Library for automation of "work in the internet for everyone" - i.e. ppc and so on

# Usage Example
```java
public class App {
	final static Logger LOG = LoggerFactory.getLogger(App.class);
	
	private static final ThreadFactory threadFactory = new ThreadFactoryBuilder()
        .setNameFormat("Byteverts-%d")
        .setDaemon(true)
        .build();
	private static ExecutorService executor = Executors.newCachedThreadPool(threadFactory);
	
	private static Future<Boolean> runWorker(ProxyProvider p, String url, String username, String email, String password) {
		final Config cfgInner = new Config();
		cfgInner.setProperty(CommonWorker.Properties.PROXY, p.getNextCheckedProxy());
		cfgInner.setProperty(BytevertsWorker.Properties.URL, url);
		cfgInner.setProperty(BytevertsWorker.Properties.USER, username);
		cfgInner.setProperty(BytevertsWorker.Properties.EMAIL, email);
		cfgInner.setProperty(BytevertsWorker.Properties.PASSWORD, password);

		return executor.submit(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				BytevertsWorker bw = new BytevertsWorker(cfgInner);
				return bw.doWork();
			}
		});
	}
	
	public static void main(String[] args) {
		PhantomJsDriverManager.getInstance().setup();
		ChromeDriverManager.getInstance().setup();
		
		Config cfg = new Config();
		
		ProxyProvider p = new ProxyProvider(cfg);
		p.doWork();
		
		HashMap<String, String> users = new HashMap<String, String>();
		users.put("...", "...");
		users.put("...", "...");
		users.put("...", "...");
		users.put("...", "...");
		users.put("...", "...");
		
		HashMap<String, Future<Boolean>> futures = new HashMap<String, Future<Boolean>>();
		
		final Config cfgInner = new Config();
		cfgInner.setProperty(CommonWorker.Properties.PROXY, p.getNextCheckedProxy());
		cfgInner.setProperty(BytevertsWorker.Properties.URL, "https://byteverts.com/index.php");
		cfgInner.setProperty(BytevertsWorker.Properties.USER, "...");
		cfgInner.setProperty(BytevertsWorker.Properties.PASSWORD, "...");		

		futures.put("local",
		executor.submit(new Callable<Boolean>() {
			public Boolean call() throws Exception {
				BytevertsWorker bw = new BytevertsWorker(cfgInner);
				return bw.doWork();
			}
		})		
		);

		for (Map.Entry<String, String> user : users.entrySet()) {
			String[] np = user.getKey().split("@");
			futures.put(user.getKey(),
				runWorker(p, "https://byteverts.com/index.php?ref=...", np[0], user.getKey(), user.getValue()));
		}

		HashMap<String, Future<Boolean>> retryFutures = new HashMap<String, Future<Boolean>>();
		retryFutures = futures;
		do {
			futures = retryFutures;
			retryFutures = new HashMap<String, Future<Boolean>>();		
			for (Map.Entry<String, Future<Boolean>> future : futures.entrySet()) {
				try {
					if (!future.getValue().get()) {
						if (users.containsKey(future.getKey())) {
							String[] np = future.getKey().split("@");
							retryFutures.put(future.getKey(),
								runWorker(p, "https://byteverts.com/index.php?ref=...", np[0], future.getKey(), users.get(future.getKey())));
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
		} while (retryFutures.size() > 0);		
	}
}
```
