#!/bin/sh
set -e
out="$HOME/caddyapps/femi"
mkdir -p "$out"
cat > "$out/Caddyfile" <<'EOF'
{
	# Bind Caddy explicitly to IPv4 wildcard address
	default_bind 0.0.0.0
}
www.femi.market, femi.market {
	handle /.well-known/matrix/* {
		reverse_proxy https://matrix.femi.market {
			header_up Host matrix.femi.market
		}
	}
	redir /kotlin /kotlin/
	handle /kotlin/* {
		uri strip_prefix /kotlin
		reverse_proxy localhost:9017
	}
	handle /api/care* {
		uri strip_prefix /api/care
		reverse_proxy localhost:9010
	}
	handle /api/motis* {
		uri strip_prefix /api/motis
		reverse_proxy localhost:9011
	}
	handle /api/travel* {
		uri strip_prefix /api/travel
		reverse_proxy localhost:9012
	}
	handle /api/match* {
		uri strip_prefix /api/match
		reverse_proxy localhost:9013
	}
	handle /api/ui* {
		uri strip_prefix /api/ui
		reverse_proxy localhost:9014
	}
	handle /api/matrix* {
		uri strip_prefix /api/matrix
		reverse_proxy localhost:9015
	}
	handle /api/localisation* {
		uri strip_prefix /api/localisation
		reverse_proxy localhost:9016
	}
	# 2. Your Web App (handles everything else)
	handle {
		reverse_proxy localhost:9000
	}
}
springcarerecruitment.com, www.springcarerecruitment.com {
	reverse_proxy localhost:9001
}
fs.femi.market {
	header Access-Control-Allow-Origin "*"
	reverse_proxy localhost:9002
}

db.femi.market {
	reverse_proxy localhost:5984
}
surreal.femi.market {
	reverse_proxy localhost:9003
}

codec.femi.market {
	reverse_proxy localhost:9004
}

meta.femi.market {
	header Access-Control-Allow-Origin "*"
	reverse_proxy localhost:9005
}
money.femi.market {
	root * /Users/u/jsapps/remittance-showcase/site
	file_server
}
remittance.femi.market {
	reverse_proxy localhost:9006
}
llm.femi.market {
	rewrite /v1{uri}

	reverse_proxy http://127.0.0.1:1234 {
		header_up Host 127.0.0.1:1234
	}
}
rota.femi.market {
	reverse_proxy localhost:9007
}
timesheet.femi.market {
	reverse_proxy localhost:9008
}
jobs.femi.market {
	reverse_proxy localhost:9009
}
EOF
cat "$out/Caddyfile"
