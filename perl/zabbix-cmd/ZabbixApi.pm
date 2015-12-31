#!/usr/bin/perl

package ZabbixApi;

use strict;
use warnings;
use vars qw($AUTOLOAD);

use JSON::RPC::Client;
use Carp;
use Data::Dumper;

sub new
{
    my $class = shift;
    my %opt = @_;

    unless (exists $opt{endpoint} && exists $opt{user} &&
            exists $opt{passwd}) {
        confess "Package->new(endpoint => _ENDPOINT_, user => _USER_,
                              passwd => _PASSWD_)"; 
    }

    $opt{cookie} = "/tmp/zabbix.api.cookie" unless($opt{cookie});
    unlink($opt{cookie}) if ($opt{reauth});
    if (-f $opt{cookie}) {
        $opt{auth} = eval { getf($opt{cookie}) };
    }

    $opt{client} = new JSON::RPC::Client;

    return bless \%opt, $class;
}

sub call
{
    my ($self, $o) = @_;
    my $params;

    if (defined $o->{auth}) {
        delete $o->{auth};
        $params = extend($o, "jsonrpc" => "2.0", "id" => uniqid());
    } else {
        unless ($self->{auth}) {
            $self->{auth} = ZabbixApi::User->new($self)->login();
        }
        $params = extend($o, "jsonrpc" => "2.0", "id" => uniqid(),
                             "auth" => $self->{auth});
    }

    my $resp = $self->{client}->call($self->{endpoint}, $params);
    if ($resp) {
        if ($resp->is_error()) {
            confess "Error: ", Dumper($resp->error_message());
        } else {
            return $resp->result();
        }
    } else {
        confess "Error: ", $self->{client}->status_line();
    }
}

sub saveauth
{
    my ($self, $auth) = @_;
    $self->{auth} = $auth;
    putf($self->{cookie}, $auth);
    return $self;
}

sub AUTOLOAD
{
    my $self = shift;

    $AUTOLOAD =~ s/.+::(.+)//;
    return if ($1 eq 'DESTROY');

    my $pkg = __PACKAGE__. "::". ucfirst($1);

    return $pkg->new($self);
}

sub extend
{
    my $o = shift;
    my %param = @_;
    while (my ($k, $v) = each(%param)) {
        $o->{$k} = $v;
    }
    return $o;
}

sub uniqid
{
    return int(rand(100));
}

sub config
{
    my ($f) = @_;
    my $obj = eval getf($f) or confess "$f error $@";
    return %{$obj};
}

sub getf
{
    my ($f) = @_;
    open(my $fh, "<", $f) or confess "$f open failed $!";
    my $c = join("", <$fh>);
    close($fh);
    return $c;
}

sub putf
{
    my ($f, $c) = @_;
    open(my $fh, ">", $f) or confess "$f open failed $!";
    print $fh $c;
    close($fh);
}

package ZabbixApi::Base;

use strict;
use warnings;

sub new
{
    my ($class, $za) = @_;
    return bless {za => $za}, $class;
}

## zabbix's update may need read first
## merge new value to old one before update

package ZabbixApi::Hostgroup;

use strict;
use warnings;
use Data::Dumper;

use parent -norequire, 'ZabbixApi::Base';

sub create
{
    my ($self, $hg) = @_;

    my $o = {
        method => "hostgroup.create",
        params => {
            name => $hg
        },
    };

    return $self->{za}->call($o);
}

sub exists
{
    my ($self, $hg) = @_;

    my $o = {
        method => "hostgroup.exists",
        params => {
            name => $hg,
        }
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;
    my @params = @_;

    my $o = {
        method => "hostgroup.get",
        params => {
            output => "extend",
            filter => {"name" => \@params},
        }
    };

    return $self->{za}->call($o);
}

sub delete
{
    my $self = shift;
    my @gids = @_;

    my $o = {
        method => "hostgroup.delete",
        params => \@gids
    };

    return $self->{za}->call($o);
}

package ZabbixApi::Template;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub create
{
    my $self = shift;
    my %opt = @_;

    my $o = {
        method => "template.create",
        params => {
            host   => $opt{name},
            groups => $opt{groupid}
        }
    };

    return $self->{za}->call($o);
}

sub exists
{
    my ($self, $name) = @_;

    my $o = {
        method => "template.exists",
        params => {
            host => $name
        }
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;
    my @params = @_;

    my $o = {
        method => "template.get",
        params => {
            output => "extend",
            filter => {
                host => \@params
            }
        }
    };

    return $self->{za}->call($o);
}

sub delete
{
    my $self = shift;
    my @tids = @_;

    my $o = {
        method => "template.delete",
        params => \@tids
    };

    return $self->{za}->call($o);
}

package ZabbixApi::Item;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub create
{
    my $self = shift;
    my %params = @_;

    my $o = {
        method => "item.create",
        params => {
            name       => $params{name},
            key_       => $params{key},
            hostid     => $params{hostid},
            type       => $params{type},
            value_type => $params{value_type},
            units      => $params{units},
            delay      => $params{delay},
            status     => $params{status},
        }
    };

    return $self->{za}->call($o);
}

sub update
{
    my $self = shift;
    my %params = @_;

    my $o = {
        method => "item.update",
        params => {
            name       => $params{name},
            itemid     => $params{iid},
            key_       => $params{key},
            type       => $params{type},
            value_type => $params{value_type},
            units      => $params{units},
            delay      => $params{delay},
            status     => $params{status},
        }
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;
    my $hostid = shift;
    my @items = @_;

    my $o = {
        method => "item.get",
        params => {
            output  => "extend",
            hostids => $hostid,
        }
    };

    my $resp = $self->{za}->call($o);

    if (@items) {
        my %filter = map {$_ => 1} @items;
        return [grep {$filter{$_->{name}}} @{$resp}];
    } else {
        return $resp;
    }
}

package ZabbixApi::Trigger;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub get
{
    my $self = shift;

    my $o = {
        method => "trigger.get",
        params => {
            output          => "extend",
            selectFunctions => "extend",
        },
    };

    return $self->{za}->call($o);
}

package ZabbixApi::Mediatype;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub create
{
    my $self = shift;
    my %params = @_;

    my %op = (
        description => $params{name},
        type        => $params{type},
        status      => $params{status},
    );

    if ($params{type} == 0) {
        $op{smtp_server} = $params{smtp_server};
        $op{smtp_helo}   = $params{smtp_helo};
        $op{smtp_email}  = $params{smtp_email};
    } elsif ($params{type} == 1) {
        $op{exec_path} = $params{script};
    } elsif ($params{type} == 2) {
        $op{gsm_modem} = $params{gsm_modem};
    }

    my $o = {
        method => "mediatype.create",
        params => \%op
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;

    my $o = {
        method => "mediatype.get",
        params => {
            output => "extend"
        }
    };

    my $resp = $self->{za}->call($o);
    if (@_) {
        my %filter = map {$_ => 1} @_;
        return [grep {$filter{$_->{description}}} @{$resp}];
    } else {
        return $resp;
    }
}

package ZabbixApi::Usergroup;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub create
{
    my $self = shift;
    my %params = @_;

    my $o = {
        method => "usergroup.create",
        params => {
            name   => $params{name},
            rights => $params{rights},
        },
        users_status => $params{status}
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;
    
    my $o = {
        method => "usergroup.get",
        params => {
            output => "extend"
        }
    };

    my $resp = $self->{za}->call($o);
    if (@_) {
        my %filter = map {$_ => 1} @_;
        return [grep {$filter{$_->{name}}} @{$resp}];
    } else {
        return $resp;
    }
}

package ZabbixApi::User;

use strict;
use warnings;

use parent -norequire, 'ZabbixApi::Base';

sub login
{
    my $self = shift;

    my $o = {
        method => "user.login",
        params => {
            user     => $self->{za}->{user},
            password => $self->{za}->{passwd}
        },
        auth   => "", 
    };

    my $auth = $self->{za}->call($o);
    $self->{za}->saveauth($auth);

    return $auth;
}

sub create
{
    my $self = shift;
    my %params = @_;
    
    my @usrgrpids = map {{usrgrpid => $_}} @{$params{usrgrps}};
    my @user_medias;

    my $o = {
        method => "user.create",
        params => {
            alias       => $params{user},
            passwd      => $params{passwd},
            type        => $params{type},
            usrgrps     => \@usrgrpids,
            user_medias => $params{medias}
        },
    };

    return $self->{za}->call($o);
}

sub get
{
    my $self = shift;

    my $o = {
        method => "user.get",
        params => {
            "output" => "extend"
        }
    };

    my $resp = $self->{za}->call($o);

    if (@_) {
        my %filter = map {$_ => 1} @_;
        return [grep {$filter{$_->{alias}}} @{$resp}]
    } else {
        return $resp;
    }
}

sub delete
{
    my $self = shift;

    my $o = {
        method => "user.delete",
        params => \@_
    };

    return $self->{za}->call($o);
}

1;
