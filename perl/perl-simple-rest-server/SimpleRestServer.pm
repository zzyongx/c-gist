package SimpleRestServer;

use strict;
use warnings;

use Data::Dumper;
use POSIX qw(setsid :sys_wait_h :errno_h);
use Socket;
use IO::Socket::INET;
use IO::File;

use constant {
    true => 1,
    false => 0
};

use constant {
    OK                    => 200,

    MovedPermanently      => 301,
    Found                 => 302,
    NotModified           => 304,

    BadRequest            => 400,
    Forbidden             => 403,
    NotFound              => 404,
    MethodNotAllowed      => 405,
    LengthRequired        => 411,
    RequestEntityTooLarge => 413,
    RequestURITooLong     => 414,
 
    InternalServerError   => 500,
    NotImplemented        => 501,
    ServiceUnavailable    => 503,
};

our %ErrorDesc = (
    OK()                    => "OK",

    MovedPermanently()      => "Moved Permanently",
    Found()                 => "Found",
    NotModified()           => "Not Modified",

    BadRequest()            => "Bad Request",
    Forbidden()             => "Forbidden",
    NotFound()              => "Not Found",
    MethodNotAllowed()      => "Method Not Allowed",
    LengthRequired()        => "Length Required",
    RequestEntityTooLarge() => "Request Enitty Too Large",
    RequestURITooLong()     => "Request URI Too Long",
 
    InternalServerError()   => "Internal Server Error",
    NotImplemented()        => "Not Implemented",
    ServiceUnavailable()    => "Service Unavailable",
);

sub errordesc
{
    my ($code) = @_;
    if (exists $ErrorDesc{$code}) {
        return $ErrorDesc{$code};
    } else {
        return $code;
    }
}

sub new 
{
    my ($class) = shift;

    my %opt = @_;
    $opt{port} = 9191 unless (exists $opt{port}); 
    $opt{verbose} = false unless (exists $opt{verbose});
    $opt{daemonize} = true unless (exists $opt{daemonize});
    $opt{timeout} = 180 unless (exists $opt{timeout});
    $opt{maxproc} = 50 unless (exists $opt{maxproc});
    $opt{pidfile} = "/var/run/restserver.pid" unless (exists $opt{pidfile});

    if (exists $opt{logfile}) {
        $opt{loghandle} = IO::File->new($opt{logfile}, "a") or
            die "open ", $opt{logfile}, " error $?";
    } else {
        $opt{loghandle} = \*STDOUT;
    }
    $opt{loghandle}->autoflush(true);

    my $lsock = IO::Socket::INET->new(
        Listen    => 128,
        LocalPort => $opt{port},
        Proto     => "tcp",
        ReuseAddr => true
    ) or die "SimpleRestServer create $@";

    $opt{lsock} = $lsock;

    return bless(\%opt, $class);
}

sub start
{
    my ($self) = shift;
    my %hooks = @_;
    $self->{hooks} = \%hooks;

    if ($self->{daemonize}) {
        daemonize(1, 1);
        writepid($self->{pidfile});
    }

    $self->{procs} = {};

    $SIG{CHLD} = sub {
        local ($!, $?);
        while ((my $pid = waitpid(-1, WNOHANG)) > 0) {
            delete $self->{procs} if (exists($self->{procs}));
        }
    };

    my $quit = 0;
    $SIG{TERM} = sub { $quit = 1; };
    $SIG{INT} = sub { $quit = 1; };

    while (!$quit) {
        my $newsock = $self->{lsock}->accept();
        if ($newsock) {
            $self->spawn($newsock)
        } elsif (errno() != EINTR) {
            die "accept error";
        }
    }
}

sub spawn
{
    my ($self, $newsock) = @_;

    $self->{sock} = $newsock;
    $self->{btime} = time();
    $self->{method} = "REQ";

    if (keys %{$self->{procs}} > $self->{maxproc}) {
        $self->{path} = "Maxproc";
        return $self->error(ServiceUnavailable);
    }

    my $pid = fork();
    if ($pid == 0) {
        # close lsock, child can restart parent
        close($self->{lsock});

        $SIG{ALRM} = sub {
            $self->{path} = "Timeout";
            $self->error(ServiceUnavailable);
            exit(1);
        };
        alarm $self->{timeout};

        $self->service();
        exit(0);
    } elsif ($pid > 0) {
        close($self->{sock});
        $self->{procs}->{$pid} = 1;
    }
}

sub service 
{
    my ($self) = @_;

    my $addr;
    my ($recv, $t);
    while (defined($self->{sock}->recv($t, 256, 0)) && $t ne '') {
        $recv .= $t;
        $self->debug("first recv '$recv'");
        if ($recv =~ /^(.+?)(\r\n|\n){2}(.*)$/s) { 
            my ($header, $body) = ($1, $3);
            return $self->parseheader($header, $body);
        } elsif (length($recv) > 4096) {
            return $self->error(RequestURITooLong);
        }
    }
}

sub parseheader
{
    my ($self, $header, $body) = @_;

    $self->debug("parse header '$header' body '$body'");

    my @lines = split /\r\n|\n/, $header;
    my ($method, $path, $ver) = split /\s+/, shift @lines;
    my %headers = map {
        my ($key, $value) = split /\s*:\s*/, $_;
        $key => $value;
    } @lines;

    if ($method eq "POST" || $method eq "PUT") {
        if (exists $headers{"Content-Length"}) {
            my $bodylen = int($headers{"Content-Length"});
            $self->debug("bodylen expect $bodylen now ". length($body));
            if (length($body) < $bodylen) {
                my $t;
                while (defined($self->{sock}->recv($t, 4096, 0)) && $t ne '') {
                    $body .= $t;
                    $self->debug("bodylen expect $bodylen now ". length($body));
                    last if (length($body) >= $bodylen);
                }
            }
        } else {
            return $self->error(LengthRequired);
        }
    }

    $self->debug("Request ", Dumper({"method" => $method, "path" => $path,
                 "headers" => join("\n", map("$_:". $headers{$_}, keys %headers)),
                 "body" => $body}));

    $self->{method} = $method;
    $self->{path}   = $path;
    $self->process(\%headers, $body);
}

sub process 
{
    my ($self, $headers, $body) = @_;
    my ($method, $path) = (lc($self->{method}), $self->{path});

    if (exists($self->{hooks}->{$method})) {
        while (my ($regex, $func) = each(%{$self->{hooks}->{$method}})) {
            if ($path =~ $regex) {
                return $self->ok(&$func($method, $path, $headers, $body));
            }
        }
        $self->error(NotFound);
    } else {
        $self->error(NotImplemented);
    }
}

sub ok
{
    my $self = shift;
    my %return = @_;

    my $code = exists $return{code} ? $return{code} : 200;
    my $desc = exists $return{desc} ? $return{desc} : errordesc($code);
    my $headers = exists $return{headers} ? $return{headers} : {};
    my $body = exists $return{body} ? $return{body} : "";

    if ($body ne "") {
        $headers->{"Content-Length"} = length($body);
    }
    $headers->{"Connection"} = "Close";

    my $header = join("\r\n", map "$_:". $headers->{$_}, keys %{$headers});

    $self->response($code, "HTTP/1.1 $code $desc\r\n$header\r\n\r\n$body");
}

sub error
{
    my ($self, $code) = @_;
    my $desc = errordesc($code);
    $self->response($code, "HTTP/1.1 $code $desc\r\nConnection: Close\r\n\r\n", 0);
}

sub response
{
    my ($self, $code, $content) = @_;
    $self->debug("Response ", $content);
    $self->accesslog($code);
    $self->{sock}->send($content, 0);
    $self->{sock}->shutdown(2);
}

sub accesslog
{
    my ($self, $code) = @_;
    my $now = time();
    my $time = scalar localtime $now;
    my $timespan = $now - $self->{btime};

    my ($port, $iaddr) = sockaddr_in($self->{sock}->peername());
    my $ip = inet_ntoa($iaddr);

    $self->{loghandle}->print("[$time] $timespan $ip ",
          $self->{method}, " ", $self->{path}, " ", $code, "\n");
}

sub debug
{
    my $self = shift;
    return unless ($self->{verbose});

    print "[debug]", @_, "\n";
}

sub daemonize
{
    my ($nochdir, $noclose) = @_;

    unless ($nochdir) {
        chdir("/") or die "can't chdir to /: $!";   
    }
    
    unless ($noclose) {
        open(STDIN,  "/dev/null") or die "can't read /dev/null: $!";    
        open(STDOUT, "/dev/null") or die "can't write /dev/null: $!";   
        open(STDERR, "/dev/null") or die "can't write /dev/null: $!";   
    }
        
    defined(my $pid = fork) or die "can't fork: $!";
    exit if $pid;
    
    setsid() or die "can't start new session: $!";
    umask 0;
}

sub writepid
{
    my $pidfile = shift;
    open(PID, ">$pidfile");
    print PID $$;
    close(PID);
}

1;
