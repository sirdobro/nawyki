{
	admin localhost:2019
}

nawyki.intmain.cc {
	header >Cache-Control "no-store"
	header >X-Content-Type-Options "nosniff"
	reverse_proxy app:8080 {
		flush_interval -1
	}
}
