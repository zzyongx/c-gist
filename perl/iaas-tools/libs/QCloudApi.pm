package QCloudApi;

use strict;
use warnings;

use Digest::SHA qw(hmac_sha256_base64);
use URI::Escape;
use LWP;
use Carp;
use Time::Piece;
use JSON::PP qw(decode_json);

use FindBin qw($Bin);
use Data::Dumper;

use vars qw($AUTOLOAD);

use lib $Bin;
use Debug;
use CloudUtil;

sub new
{
    my ($class, %opt) = @_;

    exists($opt{akey}) or confess "akey required";
    exists($opt{skey}) or confess "skey required";
    exists($opt{region}) or confess "region required";

    $opt{name} = "qcloudapi" unless (exists($opt{name}));
    $opt{debug} = 0 unless (exists($opt{debug}));

    $opt{endpoint} = "api.qingcloud.com";

    bless \%opt, $class;    
}

sub DESTROY
{
    # do nothing
}

sub AUTOLOAD
{
    my $self = shift;
    $AUTOLOAD =~ s/.+::(.+)//;
    return if ($1 eq 'DESTROY');

    my $set = shift;
    if ($set) {
        $self->{$1} = $set;
        return $self;
    } else {
        my $id = $self->{$1} or confess "$1 not exists";
        return $id;
    }
}

sub create_keypair
{
    my ($self, %opt) = @_;
    my $name = $opt{name} || $self->{name};

    my $uri = $self->builduri("GET",
        "action"       => "CreateKeyPair",
        "keypair_name" => $name,
        "zone"         => $self->{region}, 
    );

    Debug::print("create keypair $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create keypair $uri failed, ", $obj->{message};
    }

    $self->{keypair_id} = $obj->{keypair_id};
    $self->{sshkey} = $obj->{private_key};

    Debug::print("private key ", $self->{sshkey});

    return $self;
}

sub describe_keypair
{
    my ($self, %opt) = @_;

    my @optvar;
    if ($opt{keypair_id}) {
        push(@optvar, "keypairs.1" => $opt{keypair_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{keypair_id}) {
        push(@optvar, "keypairs.1" => $self->{keypair_id});        
    } else {
        push(@optvar, "search_word" => $self->{name});
    }
    
    my $uri = $self->builduri("GET",
        "action" => "DescribeKeyPairs",
        "zone"   => $self->{region},
        @optvar);

    Debug::print("describe keypair $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe keypair $uri failed, ", $obj->{message};
    }
    return $obj;
}

sub delete_keypair
{
    my ($self, %opt) = @_;
    my $kid = $opt{keypair_id} || $self->{keypair_id};

    my $uri = $self->builduri("GET",
        "action"     => "DeleteKeyPairs",
        "keypairs.1" => $kid,
        "zone"       => $self->{region},
    );

    Debug::print("delete keypair $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete keypair $uri failed, ", $obj->{message};
    }

    $self->{keypair_id_d} = $self->{keypair_id};
    delete $self->{keypair_id};

    return $self;
}

sub create_security_group
{
    my ($self, %opt) = @_;
    my $name = $opt{name} || $self->{name};

    # create security group
    my $uri = $self->builduri("GET",
        "action"              => "CreateSecurityGroup",
        "security_group_name" => $name,
        "zone"                => $self->{region},
    );

    Debug::print("create security group $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create security group $uri failed, ", $obj->{message};
    }

    $self->{security_group_id} = $obj->{security_group_id};
    # add security group rule
    $self->add_security_rule(port => 22);
    $self->add_security_rule(port => 1194, proto => "udp");
    return $self->apply_security_rule();
}

sub add_security_rule
{
    my ($self, %opt) = @_;
    
    my $sgid      = $opt{security_group_id} || $self->{security_group_id};
    my $protocol  = $opt{protocol} || "tcp";
    my $action    = $opt{action} || "accept";
    my $priority  = $opt{priority} || 0;
    my $direction = $opt{direction} || 0;
    my $port      = $opt{port} or confess "required port";
    my $srcip     = $opt{srcip} || "";
    
    my $uri = $self->builduri("GET",
        "action"            => "AddSecurityGroupRules",
        "security_group"    => $sgid,
        "rules.1.protocol"  => $protocol,
        "rules.1.priority"  => $priority,
        "rules.1.action"    => $action,
        "rules.1.direction" => $direction,
        "rules.1.val1"      => $port,
        "rules.1.val3"      => $srcip,
        "zone"              => $self->{region},
    );

    Debug::print("authorize security group $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "authorize security group $uri failed, ", $obj->{message};
    }

    return $obj;
}

sub apply_security_rule
{
    my ($self, %opt) = @_;
    my $sgid = $opt{security_group_id} || $self->{security_group_id};

    my $uri = $self->builduri("GET",
        "action"         => "ApplySecurityGroup",
        "security_group" => $sgid,
        "zone"           => $self->{region},
    );

    Debug::print("apply security group $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "apply security group $uri failed, ", $obj->{message};
    }

    return $self;
}

sub security_rule_exists
{
    my ($self, %opt) = @_;

    my $sgid      = $opt{security_group_id} || $self->{security_group_id};
    my $proto     = $opt{protocol} || "tcp";
    my $action    = $opt{action} || "accept";
    my $direction = $opt{direction} || 0;
    my $port      = $opt{port} or confess "required port";
    my $srcip     = $opt{srcip} || "";

    my $uri = $self->builduri("GET",
        "action"         => "DescribeSecurityGroupRules",
        "security_group" => $sgid,
        "direction"      => $direction,
        "zone"           => $self->{region},
    );

    Debug::print("query security rule $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "query security rule $uri failed, ", $obj->{message};
    }

    foreach my $rule (@{$obj->{security_group_rule_set}}) {
        $rule->{val3} = "" unless (exists $rule->{val3});
        if ($rule->{protocol} eq $proto && $rule->{action} eq $action &&
                $rule->{val1} == $port && $rule->{val2} == $port &&
                    $rule->{val3} == $srcip) {
            return 1;
        }
    }

    return 0;
}

sub describe_security_group
{
    my ($self, %opt) = @_;

    my @optvar;
    if ($opt{security_group_id}) {
        push(@optvar, "security_groups.1" => $opt{security_group_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{security_group_id}) {
        push(@optvar, "security_groups.1" => $self->{security_group_id});        
    } else {
        push(@optvar, "search_word" => $self->{name});
    }

    my $uri = $self->builduri("GET",
        "action" => "DescribeSecurityGroups",
        "zone"   => $self->{region},
        @optvar);

    Debug::print("describe security group $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe security group $uri failed, ", $obj->{message};
    }
    return $obj;
}

sub delete_security_group
{
    my ($self, %opt) = @_;
    my $sgid = $opt{security_group_id} || $self->{security_group_id};
    
    my $uri = $self->builduri("GET",
        "action"            => "DeleteSecurityGroups",
        "security_groups.1" => $sgid,
        "zone"              => $self->{region},
    );

    Debug::print("delete security group $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete security group $uri failed, ", $obj->{message};
    }

    $self->{security_group_id_d} = $self->{security_group_id};
    delete $self->{security_group_id};

    return $self;
}

sub create_router
{
    my ($self, %opt) = @_;
    my $sgid = $opt{security_group_id} || $self->{security_group_id};

    my $uri = $self->builduri("GET",
        "action"         => "CreateRouters",
        "router_name"    => $self->{name},
        "security_group" => $sgid,
        "zone"           => $self->{region}, 
    );

    Debug::print("create router $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create router $uri failed, ", $obj->{message};
    }

    $self->{router_id} = $obj->{routers}->[0];
    return $self;
}

sub router_bind_eip
{
    my ($self, %opt) = @_;
    my $eipid = $opt{eip_id} || $self->{eip_id};
    my $rid = $opt{router_id} || $self->{router_id};

    my $uri = $self->builduri("GET",
        "action" => "ModifyRouterAttributes",
        "router" => $rid,
        "eip"    => $eipid,
        "zone"   => $self->{region},
    );

    Debug::print("router bind eip");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "router bind eip $uri failed ", $obj->{message};
    }

    return $self;
} 

sub delete_router
{
    my ($self, %opt) = @_;
    my $rid = $opt{router_id} || $self->{router_id};

    my $uri = $self->builduri("GET",
        "action" => "DeleteRouters",
        "routers.1" => $rid,
        "zone"      => $self->{region},
    );
 
    Debug::print("delete router $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete router $uri failed, ", $obj->{message};
    }

    $self->{router_id_d} = $self->{router_id};
    delete $self->{router_id};

    return $self;
}

sub wait_router_delete
{
    my ($self, %opt) = @_;
    $opt{router_id} = $self->{router_id_d} unless (%opt);

    my $func = sub {
        my $obj = $self->describe_router(%opt);
        my $o = $obj->{router_set}->[0];
        return $o->{status} eq "deleted" && $o->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait router delete timeout";
    }

    return $self;
}

sub create_vxnet
{
    my ($self, %opt) = @_;
    my $name = $opt{name} || $self->{name};

    my $uri = $self->builduri("GET",
        "action" => "CreateVxnets",
	"vxnet_type" => 1,
	"vxnet_name" => $name,
	"zone"       => $self->{region},
    );
    
    Debug::print("create vxnet $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
	confess "create vxnet $uri failed ", $obj->{message};
    }

    $self->{vxnet_id} = $obj->{vxnets}->[0];
    return $self;
}

sub describe_vxnet
{
    my ($self, %opt) = @_;

    my @optvar;
    if ($opt{vxnet_id}) {
        push(@optvar, "vxnets.1" => $opt{vxnet_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{vxnet_id}) {
        push(@optvar, "vxnets.1" => $self->{vxnet_id});
    } else {
        push(@optvar, "search_word" => $self->{name});
    }

    my $uri = $self->builduri("GET",
        "action" => "DescribeVxnets",
	"zone"     => $self->{region},
        @optvar);

    Debug::print("describe vxnet $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
	confess "describe vxnet $uri failed ", $obj->{message};
    }

    return $obj;
}

sub join_router
{
    my ($self, %opt) = @_;
    my $nid = $opt{vxnet_id} || $self->{vxnet_id};
    my $rid = $opt{router_id} || $self->{router_id};
    my $cidr = $opt{cidr} || "192.168.1.0/24";

    my $uri = $self->builduri("GET",
        "action"     => "JoinRouter",
        "vxnet"      => $nid,
	"router"     => $rid,
        "ip_network" => uri_escape($cidr),
        "zone"       => $self->{region},
    );

    Debug::print("join router $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
	confess "join router $uri failed, ", $obj->{message};
    }

    return $self;
}

sub delete_vxnet
{
    my ($self, %opt) = @_;
    my $id = $opt{vxnet_id} || $self->{vxnet_id};

    my $uri = $self->builduri("GET",
        "action" => "DeleteVxnets",
        "vxnets.1" => $id,
        "zone"     => $self->{region},
    );

    Debug::print("delete vxnet $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete vxnet $uri failed, ", $obj->{message};
    }

    $self->{vxnet_id_d} = $self->{vxnet_id};
    delete $self->{vxnet_id};

    return $self;
}

# vpn api is useless, vpn certificate interface is not provided
sub create_vpn
{
    my ($self, %opt) = @_;
    my $rid = $opt{router_id} || $self->{router_id};
    
    my $uri = $self->builduri("GET",
        "action"                => "AddRouterStatics",
        "router"                => $rid,
        "statics.1.static_type" => 2,
        "statics.1.val1"        => "openvpn",
        "zone"                  => $self->{region},
    );

    Debug::print("create vpn $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create vpn $uri failed, ", $obj->{message};
    }

    $self->{vpn_id} = $obj->{router_statics}->[0];

    $uri = $self->builduri("GET",
        "action"           => "DescribeRouterStatics",
        "router_statics.1" => $self->{vpn_id},
        "zone"             => $self->{region}, 
    );

    Debug::print("get vpn config $uri");
    
    $obj = httpcall(get => $uri);

    return $self;
}

sub create_port_forward
{
    my ($self, %opt) = @_;

    my $rid   = $opt{router_id} || $self->{router_id};
    my $name  = $opt{name} || $self->{name};
    my $sport = $opt{sport} or confess "sport required";
    my $dstip = $opt{dstip} or confess "dip required";
    my $dport = $opt{dport} || 22;
    my $proto = $opt{protocol} || "tcp";
    
    my $uri = $self->builduri("GET",
        "action"                       => "AddRouterStatics",
        "router"                       => $rid,
        "statics.1.router_static_name" => $name,
        "statics.1.static_type"        => 1,
        "statics.1.val1"               => $sport,
        "statics.1.val2"               => $dstip,
        "statics.1.val3"               => $dport,
        "statics.1.val4"               => $proto,
        "zone"                         => $self->{region}, 
    );

    Debug::print("create port forward $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create port forward $uri failed, ", $obj->{message};
    }

    $self->add_security_rule(port => $sport);
    $self->apply_security_rule();
                           
    return $self;
}

sub port_forward_exists
{
    my ($self, %opt) = @_;

    my $rid = $opt{router_id} || $self->{router_id};
    my $sport = $opt{sport} or confess "sport required";
    my $dstip = $opt{dstip} or confess "dip required";
    my $dport = $opt{dport} || 22;

    my $uri = $self->builduri("GET",
        "action"      => "DescribeRouterStatics",
        "router"      => $rid,
        "static_type" => 1,
        "zone"        => $self->{region},
    );

    Debug::print("query port forward $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "query port forward $uri failed, ", $obj->{message};
    }

    my $exists = 0;
    my @conflits;
    foreach my $rule (@{$obj->{router_static_set}}) {
        if ($rule->{val1} == $sport && $rule->{val2} eq "$dstip" &&
            $rule->{val3} == $dport) {
            $exists = 1;
        } elsif ($rule->{val1} == $sport) {
            push(@conflits, $rule->{router_static_id});
        }
    }

    if (@conflits) {
        $self->delete_conflit_port_forward(@conflits);
    }
    return $exists;
}

sub delete_conflit_port_forward
{
    my $self = shift;
    my @Ids = @_;
    
    my @rtrs;
    for (my $i = 0; $i < @Ids; $i++) {
        my $index = $i+1;
        push(@rtrs, "router_statics.$index" => $Ids[$i]);
    }
        
    my $uri = $self->builduri(
        "GET",
        "action" => "DeleteRouterStatics",
        "zone"   => $self->{region},
        @rtrs);

    Debug::print("delete conflict port forward $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete conflict port forward $uri failed, ", $obj->{message};
    }
    
    return $self;
}

sub describe_router
{
    my ($self, %opt) = @_;
    
    my @optvar;
    if ($opt{router_id}) {
        push(@optvar, "routers.1" => $opt{router_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{router_id}) {
        push(@optvar, "routers.1" => $self->{router_id});
    } else {
        push(@optvar, "search_word" => $self->{name});
    }
    
    my $uri = $self->builduri("GET",
        "action"    => "DescribeRouters",
        "zone"      => $self->{region},
        @optvar);

    Debug::print("describe router $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe router $uri failed, ", $obj->{message};
    }

    return $obj;
}

sub wait_router_active
{
    my ($self, %opt) = @_;
    my $rid = $opt{router_id} || $self->{router_id};

    my $func = sub {
        my $obj = $self->describe_router(%opt);
        return $obj->{router_set}->[0]->{status} eq "active" &&
               $obj->{router_set}->[0]->{transition_status} eq "";
    };
    
    unless (CloudUtil::loop_call($func)) {
        confess "wait create router timeout";
    }

    return $self;
}    

sub update_router
{
    my ($self, %opt) = @_;
    my $rid = $opt{router_id} || $self->{router_id};

    my $uri = $self->builduri("GET",
        "action"    => "UpdateRouters",
        "routers.1" => $rid,
        "zone"      => $self->{region},
    );
       
    Debug::print("update router $uri");
      
    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "update router $uri failed, ", $obj->{message};
    }

    return $self;
}

sub wait_router_updated
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_router(%opt);
        return $obj->{router_set}->[0]->{status} eq "active" &&
               $obj->{router_set}->[0]->{is_applied} &&
               $obj->{router_set}->[0]->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "update router timeout";
    }

    return $self;
}

sub create_instance
{
    my ($self, %opt) = @_;

    exists($opt{image_id}) or confess "image id required";
    exists($opt{type}) or confess "instance type required";

    my $kid  = $opt{keypair_id} || $self->{keypair_id};
    my $nid  = $opt{vxnet_id} || $self->{vxnet_id} || "vxnet-0";
    my $sgid = $opt{security_group_id} || $self->{security_group_id};
    my $name = $opt{name} || $self->{name};

    # run instance
    my $uri = $self->builduri("GET",
        "action"         => "RunInstances",
        "image_id"       => $opt{image_id},
        "instance_name"  => $name,
        "instance_type"  => $opt{type},
        "login_mode"     => "keypair",
        "login_keypair"  => $kid,
        "vxnets.1"       => $nid,
        "security_group" => $sgid,
        "zone"           => $self->{region},
    );

    Debug::print("run instance $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "run instance $uri failed, ", $obj->{message};
    }

    $self->{instance_id} = $obj->{instances}->[0];
    return $self;
}

sub describe_instance
{
    my ($self, %opt) = @_;
    
    my @optvar;
    if ($opt{instance_id}) {
        push(@optvar, "instances.1" => $opt{instance_id});        
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{instance_id}) {
        push(@optvar, "instances.1" => $self->{instance_id});
    } else {
        push(@optvar, "search_word" => $self->{name});
    }
    
    my $uri = $self->builduri("GET",
        "action" => "DescribeInstances",
        "zone"   => $self->{region},
        @optvar);

    Debug::print("describe instance $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe  instance $uri failed, ", $obj->{message};
    }
    return $obj;
}

sub wait_instance_running
{
    my ($self, %opt) = @_;

    my $func = sub {
	my $obj = $self->describe_instance(%opt);
	my $o = $obj->{instance_set}->[0];
        return $o->{status} eq "running" && $o->{transition_status} eq "" &&
               $o->{vxnets}->[0]->{private_ip};
    };

    unless (CloudUtil::loop_call($func)) {
	confess "wait instance create timeout";
    }
    return $self;
}

sub instance_change_ip
{
    my ($self, %opt) = @_;

    my $ip = $opt{ip} or confess "ip required";
    my $iid = $opt{instance_id} || $self->{instance_id};
    my $rid = $opt{router_id} || $self->{router_id};

    my $uri = $self->builduri("GET",
        "action"                => "AddRouterStatics",
        "router"                => $rid,
	"statics.1.static_type" => 3,
        "statics.1.val1"        => $iid,
        "statics.1.val2"        => uri_escape("fixed-address=".$ip),
        "zone"                  => $self->{region}, 
    );

    Debug::print("change instance ip $uri");
    
    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
	confess "change instance ip $uri failed, ", $obj->{message};
    }

    $self->{fixed_address} = $ip;
    return $self;
}

sub wait_instance_change_ip
{
    my ($self, %opt) = @_;

    $self->wait_router_active();
    $self->update_router(%opt)->wait_router_updated();
    $self->restart_instance(%opt)->wait_instance_running(%opt);

    my $ip = $opt{fixed_address} || $self->{fixed_address};

    # sometimes ip wouldn't change when restart, try again
    for (my $i = 0; $i < 3; $i++) {
        my $obj = $self->describe_instance(%opt);
        if ($obj->{instance_set}->[0]->{vxnets}->[0]->{private_ip} eq $ip) {
            last;
        } else {
            $self->restart_instance(%opt)->wait_instance_running(%opt);
        }
    }

    return $self;
}

sub restart_instance
{
    my ($self, %opt) = @_;
    my $id = $opt{instance_id} || $self->{instance_id};
    
    my $uri = $self->builduri("GET",
        "action" => "RestartInstances",
        "instances.1" => $id,
        "zone"        => $self->{region},
    );  

    Debug::print("restart instance $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "restart instance $uri failed, ", $obj->{message};
    }

    return $self;
}

sub stop_instance
{
    my ($self, %opt) = @_;
    
    my $id = $opt{instance_id} || $self->{instance_id};
    my $force = exists $opt{force} ? $opt{force} : 0;
    
    my $uri = $self->builduri("GET",
        "action"      => "StopInstances",
        "instances.1" => $id,
        "force"       => $force,
        "zone"        => $self->{region},
    );

    Debug::print("stop instance $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "stop instance $uri failed, ", $obj->{message};
    }
    
    return $self;
}

sub wait_instance_stop
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_instance(%opt);
        my $o = $obj->{instance_set}->[0];
        return $o->{status} eq "stopped" && $o->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait instance stop timeout";
    }
    return $self;
}

sub delete_instance
{
    my ($self, %opt) = @_;
    my $id = $opt{instance_id} || $self->{instance_id};

    my $uri = $self->builduri("GET",
        "action"      => "TerminateInstances",
        "instances.1" => $id,
        "zone"        => $self->{region},
    );

    Debug::print("delete instance $uri");

    my $obj = httpcall(get => $uri);

    unless ($obj->{ret_code} == 0) {
        confess "delete instance $uri failed, ", $obj->{message};
    }

    $self->{instance_id_d} = $self->{instance_id};
    delete $self->{instance_id};

    return $self;
}

sub wait_instance_delete
{
    my ($self, %opt) = @_;
    $opt{instance_id} = $self->{instance_id_d} unless (%opt);
    
    my $func = sub {
        my $obj = $self->describe_instance(%opt);
        my $o = $obj->{instance_set}->[0];
        return $o->{status} eq "terminated" && $o->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait instance delete timeout";
    }
    return $self;
}

sub allocate_eip
{
    my ($self, %opt) = @_;

    my $bandwidth = $opt{bandwidth} || 1;
    my $name      = $opt{name} || $self->{name};

    my $uri = $self->builduri("GET",
        "action"    => "AllocateEips",
        "bandwidth" => $bandwidth,
        "eip_name"  => $name,
        "zone"      => $self->{region}
    );

    Debug::print("allocate eip $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "allocate instance $uri failed, ", $obj->{message};
    }
    
    $self->{eip_id} = $obj->{eips}->[0];
    return $self;
}

sub describe_eip                             
{
    my ($self, %opt) = @_;

    my @optvar;
    if ($opt{eip_id}) {
        push(@optvar, "eips.1" => $opt{eip_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{eip_id}) {
        push(@optvar, "eips.1" => $self->{eip_id});
    } else {
        push(@optvar, "search_word" => $self->{name});
    }

    my $uri = $self->builduri("GET",
        "action" => "DescribeEips",
        "zone"   => $self->{region}, 
        @optvar);

    Debug::print("describe eip $uri");
    
    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe eip $uri failed, ", $obj->{message};
    }

    return $obj;
}
                              
sub associate_eip
{
    my ($self, %opt) = @_;

    my $eipid = $opt{eip_id} || $self->{eip_id};
    my $id    = $opt{instance_id} || $self->{instance_id};

    # bind eip, instance's status must be running
    my $uri = $self->builduri("GET",
        "action"   => "AssociateEip",
        "eip"      => $eipid,
        "instance" => $id,
        "zone"     => $self->{region},
    );

    Debug::print("associate eip $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "associate eip $uri failed, ", $obj->{message};
    }

    return $self;
}

sub wait_eip_available
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_eip(%opt);
        my $o = $obj->{eip_set}->[0];
        if ($o->{status} eq "available" && $o->{transition_status} eq "") {
            $self->{eip} = $obj->{eip_set}->[0]->{eip_addr};
            return 1;
        } else {
            return 0;
        }
    };
    
    unless (CloudUtil::loop_call($func)) {
        confess "wait eip available timeout";
    }
    return $self;
}

sub wait_eip_associated
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_eip(%opt);
        my $o = $obj->{eip_set}->[0];
        return $o->{status} eq "associated" && $o->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait eip associated timeout";
    }
    return $self;
}

sub release_eip
{
    my ($self, %opt) = @_;
    my $id = $opt{eip_id} || $self->{eip_id};

    my $uri = $self->builduri("GET",
        "action" => "ReleaseEips",
        "eips.1" => $id,
        "zone"   => $self->{region},
    );

    Debug::print("release eip $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "release eip $uri failed, ", $obj->{message};
    }

    delete $self->{eip};
    $self->{eip_id_d} = $self->{eip_id};
    delete $self->{eip_id};

    return $self;
}

sub create_image
{
    my ($self, %opt) = @_;
    
    my $id = $opt{instance_id} || $self->{instance_id};
    my $name = $opt{name} || $self->{name};
    
    my $uri = $self->builduri("GET",
        "action"     => "CaptureInstance",
        "instance"   => $id,
        "image_name" => $name,
        "zone"       => $self->{region},
    );
    
    Debug::print("create image $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "create image $uri failed, ", $obj->{message};
    }

    $self->{image_id} = $obj->{image_id};
    return $self;
}

sub describe_image
{
    my ($self, %opt) = @_;
    
    my @optvar;
    if ($opt{image_id}) {
        push(@optvar, "images.1" => $opt{image_id});
    } elsif ($opt{name}) {
        push(@optvar, "search_word" => $opt{name});
    } elsif ($self->{image_id}) {
        push(@optvar, "images.1" => $self->{image_id});
    } else {
        push(@optvar, "search_word" => $self->{name});
    }

    my $uri = $self->builduri("GET",
        "action" => "DescribeImages",
        "zone"   => $self->{region}, 
        @optvar); 

    Debug::print("describe image $uri");
    
    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "describe image $uri failed, ", $obj->{message};
    }

    return $obj;
}

sub wait_image_available
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_image(%opt);
        my $o = $obj->{"image_set"}->[0];
        return $o->{status} eq "available" && $o->{transition_status} eq "";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait image available timeout";
    }
    return $self;        
}

sub delete_image
{
    my ($self, %opt) = @_;

    my $uri = $self->builduri("GET",
        "action"   => "DeleteImagesResponse",
        "images.1" => ($opt{image_id} || $self->{image_id}),
        "zone"     => $self->{region},
    );

    Debug::print("delete image $uri");

    my $obj = httpcall(get => $uri);
    unless ($obj->{ret_code} == 0) {
        confess "delete image $uri failed, ", $obj->{message};
    }

    return $obj;
}

sub destroy
{
    my $self = shift;
    my %filter = map {$_ => 1} @_;
    
    my %opt = (name => $self->{name});

    $opt{name} or confess "name can't be null";

    my $obj;
    if ($filter{instance}) {
        $obj = $self->describe_instance(%opt);
        foreach my $o (@{$obj->{instance_set}}) {
            if ($o->{status} eq "stopped" || $o->{status} eq "running") {
                $self->delete_instance(instance_id => $o->{instance_id});
                $self->wait_instance_delete(instance_id => $o->{instance_id});
            }
        }
    }

    if ($filter{router}) {
        $obj = $self->describe_router(%opt);
        foreach my $o (@{$obj->{router_set}}) {
            if ($o->{status} eq "active" || $o->{status} eq "poweroffed") {
                $self->delete_router(router_id => $o->{router_id});
                $self->wait_router_delete(router_id => $o->{router_id});
            }
        }
    }

    if ($filter{vxnet}) {
        $obj = $self->describe_vxnet(%opt);
        foreach my $o (@{$obj->{vxnet_set}}) {
            $self->delete_vxnet(vxnet_id => $o->{vxnet_id});
        }
    }

    if ($filter{eip}) {
        $obj = $self->describe_eip(%opt);
        foreach my $o (@{$obj->{eip_set}}) {
            if ($o->{status} eq "available" || $o->{status} eq "associated") {
                $self->release_eip(eip_id => $o->{eip_id});
            }
        }
    }

    if ($filter{security_group}) {
        $obj = $self->describe_security_group(%opt);
        foreach my $o (@{$obj->{security_group_set}}) {
            $self->delete_security_group(security_group_id => $o->{security_group_id});
        }
    }        
    
    if ($filter{keypair}) {
        $obj = $self->describe_keypair(%opt);
        foreach my $o (@{$obj->{keypair_set}}) {
            $self->delete_keypair(keypair_id => $o->{keypair_id});
        }
    }

    if ($filter{image}) {
        my $obj = $self->describe_image(%opt);
        foreach my $o (@{$obj->{image_set}}) {
            $self->delete_image(image_id => $o->{image_id});
        }
    }

    return $self;
}


sub builduri
{
    my ($self, $method, %kv) = @_;
    my @qs = map { "$_=". $kv{$_} } keys(%kv);

    my $endpoint = $self->{endpoint};

    push(@qs, "access_key_id=". $self->{akey});
    push(@qs, "time_stamp=". CloudUtil::iso8601());
    push(@qs, "version=1");
    push(@qs, "signature_method=HmacSHA256");
    push(@qs, "signature_version=1");
    push(@qs, "signature=". $self->signature($method, "/iaas/", @qs));

    return "https://$endpoint/iaas/?". join("&", @qs);
}

sub signature
{
    my ($self, $method, $path, @qs) = @_;
    my $data = join("\n", $method, $path, join("&", sort @qs));
    my $digest = hmac_sha256_base64($data, $self->{skey});
    $digest .= "=" while (length($digest) % 4);
    return uri_escape($digest);
}

sub httpcall
{
    my %opt = @_;

    my ($method, $uri);
    if (exists($opt{get})) {
        ($method, $uri) = ('GET', $opt{get});
    } elsif (exists($opt{post})) {
       ($method, $uri) = ('POST', $opt{post});
    } else {
        die "http method required";
    }

    my $agent = LWP::UserAgent->new();
    my $request = HTTP::Request->new($method => $uri);
    my $response = $agent->request($request);

    my $body = $response->content();
    Debug::verbose("json string $body");
    my $obj = eval { decode_json($body) };
    if ($obj) {
        Debug::verbose("xml obj ", Dumper($obj));
        return $obj;
    } else {
        confess Dumper($response);
    }
}

1;
