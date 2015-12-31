package RemoteRun;

use strict;
use warnings;

use Carp;
use FindBin qw($Bin);
use IO::Socket::INET;

use lib $Bin;
use Debug;

sub new
{
    my ($class, %opt) = @_;

    exists $opt{sshkey} or confess "sshkey required";
    
    $opt{user} = "root" unless (exists $opt{user});
    $opt{port} = $opt{port} || 22;
    $opt{sudo} = 1 unless (exists $opt{sudo} || $opt{user} eq "root");
    $opt{retry} = 1 unless (exists $opt{retry});
    $opt{wait} = 30 unless (exists $opt{wait});
    my $name = $opt{name} || time();

    mkdir("/var/lib/remoterun");
    
    my $keyfile = "/var/lib/remoterun/$name.pem";
    open(my $fh, ">", $keyfile);
    print $fh $opt{sshkey};
    close($fh);
    chmod(0400, $keyfile);

    $opt{sshopt} = "-o StrictHostKeyChecking=no -i $keyfile";

    return bless \%opt, $class;    
}

sub sudo { return $_[0]->{sudo} || 0; }

sub rcp
{
    my ($self, %opt) = @_;

    %opt = merge_opt(\%opt, $self,
                     ["port", "user", "retry", "wait"],
                     ["src", "dst", "host"]);

    my $sshopt = $self->{sshopt};
    my $cmd;
    if ($opt{user} eq "root") {
        $cmd = "/usr/bin/scp -P ${opt{port}} $sshopt ${opt{src}} ${opt{user}}\@${opt{host}}:${opt{dst}}";
    } else {
        my $t = "/tmp/remoterun_KTU0584X7grAGcQSjqeakRvklB0";
        $cmd = "/usr/bin/scp -P ${opt{port}} $sshopt ${opt{src}} ${opt{user}}\@${opt{host}}:$t && /usr/bin/ssh -p ${opt{port}} ${opt{user}}\@${opt{host}} $sshopt -t 'sudo cp $t ${opt{dst}} && sudo rm $t'";
    }

    Debug::print("rcp $cmd");
    
    my $i;
    for ($i = 0; $i < $opt{retry}; $i++) {
        system($cmd);
        last if ($? == 0);
        sleep($opt{wait}) if ($i+1 < $opt{retry});
    }
    if ($i == $opt{retry}) {
        confess "rcp $cmd failed";
    }

    return $self;    
}

sub run
{
    my ($self, %opt) = @_;

    %opt = merge_opt(\%opt, $self,
                     ["port", "user", "sudo", "retry", "wait"],
                     ["cmd", "host"]);
    
    my $sshopt = $self->{sshopt};
    $sshopt .= " -t" if ($opt{sudo});

    my $env = "";
    while (my ($k, $v) = each(%{$opt{env}})) {
        $env .= "$k=\"$v\" ";
    }

    my $sudo = $opt{sudo} ? "sudo" : "";
    
    my $run = "/usr/bin/ssh -p ${opt{port}} ${opt{user}}\@${opt{host}} $sshopt '$sudo $env ${opt{cmd}}'";
    Debug::print("run $run");

    my $i;
    for ($i = 0; $i < $opt{retry}; $i++) {
        system($run);
        last if ($? == 0);
        sleep($opt{wait}) if ($i+1 < $opt{retry});
    }
    if ($i == $opt{retry}) {
        confess "run $run failed";
    }

    return $self;
}

sub ensure_connection
{
    my ($self, %opt) = @_;

    %opt = merge_opt(\%opt, $self,
                     ["port", "retry", "wait"],
                     ["host"]);
    my $i;
    for ($i = 0; $i < $opt{retry}; $i++) {
        my $sock = IO::Socket::INET->new(PeerAddr => $opt{host},
                                         PeerPort => $opt{port},
                                         Proto    => 'tcp');
        last if ($sock);
        sleep($opt{wait}) if ($i+1 < $opt{retry});
    }
    if ($i == $opt{retry}) {
        confess "connect ${opt{host}}:${opt{port}} failed";
    }

    return $self;
}

sub merge_opt
{
    my $opt = shift;
    my $default = shift;
    my $optional = shift;
    my $required = shift || [];

    foreach my $key (@$required) {
        $opt->{$key} = ($opt->{$key} || $default->{$key}) or confess "$key required";
    }
    foreach my $key (@$optional) {
        $opt->{$key} = $opt->{$key} || $default->{$key};
    }
    return %$opt;
}

1;
