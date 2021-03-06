perl-simple-rest-server
=======================

a simple rest server implemented by perl, very simple.
when request matches the http method and http path regex,
fork a new process, exec callback function.

usage
=====

# options:
# port      listen port
# maxproc   max process allowed at the same time
# timeout   process exec timeout
# pidfile   when daemonize true, write pid to pidfile
# logfile   log file
# daemonize run as daemon or not
# verbose   output debug info

my $server = SimpleRestServer->new(pidfile   => "/var/run/demo.pid",
                                   port      => 80,
                                   verbose   => 1,
                                   daemonize => 1);
# cb match list
# when method is get, and path =~ |^/test/([^/]+)$|, call echo
$server->start(
    get => {
        qr|^/test/([^/]+)$| => \&echo
    },
);

# cb param ($method, $path, $headers, $body) 
# cb return (headers => {}, body => "", code => 200, desc => "OK")
# if the key(headers, code, desc) is ignore, use default
sub echo
{
    my ($method, $path, $headers, $body) = @_;
    my $header = join("\n", map("$_:". $headers->{$_}, keys %{$headers}));
    return (headers => {"Demo-X-Trace" => "0.0.0.0"},
            body => "method: $method\npath: $path\n$header\n$body", 
           ); 
}

# ./demo
# curl "http://127.0.0.1/test/x" -vv
