package AwsApi;

use strict;
use warnings;

use Digest::SHA qw(hmac_sha256_base64);
use URI::Escape;
use LWP;
use XML::Simple qw(:strict);
use Carp;
use Data::Dumper;

use FindBin qw($Bin);
use lib $Bin;
use vars qw($AUTOLOAD);

use CloudUtil;
use Debug;

sub new
{
    my ($class, %opt) = @_;

    exists($opt{akey}) or confess "akey required";
    exists($opt{skey}) or confess "skey required";
    exists($opt{region}) or confess "region required";

    $opt{name} = __PACKAGE__ unless (exists($opt{name}));
    $opt{debug} = 0 unless(exists($opt{debug}));
    $opt{Domain} = "vpc" unless (exists($opt{Domain}));

    $opt{endpoint} = ec2_endpoint($opt{region});

    $opt{sshkey} = undef;
    $opt{sgid} = undef;

    bless \%opt, $class;
}

sub DESTROY
{
    # nothing
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

sub tag 
{
    my ($self, %opt) = @_;

    $opt{id} or confess "id required";
    
    my $id = $opt{id};
    delete $opt{id};

    my ($key, $value) = each(%opt);
    $key = "Name" unless ($key);
    $value = $self->{name} unless ($value);

    my $uri = $self->builduri("GET",
        "Action"       => "CreateTags",
        "ResourceId.1" => $id,
        "Tag.1.Key"    => $key,
        "Tag.1.Value"  => $value,
    );

    Debug::print("tag $uri");

    my $error;
    my $func = sub {
        my $obj = httpcall(get => $uri);
        if (is_error($obj)) {
            $error = error_msg($obj);
            return 0;
        } else {
            return 1;
        }
    };
    
    unless (CloudUtil::loop_call($func)) {
        confess "tag $uri failed $error";
    }

    return $self;
}

sub create_vpc
{
    my ($self, %opt) = @_;
    
    my $cidr = $opt{CidrBlock} || "192.168.0.0/16";
    my $tenancy = $opt{InstanceTenancy} ? $opt{InstanceTenancy} : "default";

    my $uri = $self->builduri("GET",
        "Action"          => "CreateVpc",
        "CidrBlock"       => uri_escape($cidr),
        "InstanceTenancy" => $tenancy,
    );

    Debug::print("create vpc $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create vpc $uri failed, ", error_msg($obj);
    }

    $self->{VpcId} = $obj->{vpc}->{vpcId};
    return $self->tag(id => $self->{VpcId});
}

sub describe_vpc
{
    my ($self, %opt) = @_;
    my $vpcid = $opt{VpcId} || $self->{VpcId};

    my $uri = $self->builduri("GET",
        "Action"  => "DescribeVpcs",
        "VpcId.1" => $vpcid,
    );

    Debug::print("describe vpc $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe vpc $uri failed, ", error_msg($obj);
    }

    return $obj;
}

sub delete_vpc
{
    my ($self, %opt) = @_;

    my $vpcid = $opt{VpcId} || $self->{VpcId};
    
    my $uri = $self->builduri("GET",
        "Action" => "DeleteVpc",
        "VpcId"  => $vpcid,
    );

    Debug::print("delete vpc $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete vpc $uri failed, ", error_msg($obj);
    }

    unless ($opt{VpcId}) {
        $self->{VpcIdD} = $self->{VpcId};
        delete $self->{VpcId};
    }

    return $self;
}

# wait vpc active is unnecessary
sub create_subnet
{
    my ($self, %opt) = @_;

    my $vpcid = $opt{VpcId} || $self->{VpcId};
    my $cidr = $opt{CidrBlock} || "192.168.1.0/24";

    my $uri = $self->builduri("GET",
        "Action"    => "CreateSubnet",
        "VpcId"     => $vpcid,
        "CidrBlock" => uri_escape($cidr),
    );

    Debug::print("create subnet $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create subnet $uri failed, ", error_msg($obj);
    }

    $self->{SubnetId} = $obj->{subnet}->{subnetId};
    return $self->tag(id => $self->{SubnetId});
}

sub describe_subnet
{
    my ($self, %opt) = @_;
    my $id = $opt{SubnetId} || $self->{SubnetId};

    my $uri = $self->builduri("GET",
        "Action"     => "DescribeSubnets",
        "SubnetId.1" => $id);

    Debug::print("describe subnet $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create subnet $uri failed, ", error_msg($obj);
    }

    return $obj;
}

sub wait_subnet_available
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_subnet(%opt);
        return $obj->{subnetSet}->{item}->[0]->{state} eq "available";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait subnet available timeout";
    }

    return $self;
}

sub delete_subnet
{
    my ($self, %opt) = @_;

    my $id = $opt{SubnetId} || $self->{SubnetId};

    my $uri = $self->builduri("GET",
        "Action"   => "DeleteSubnet",
        "SubnetId" => $id);

    Debug::print("delete subnet $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete subnet $uri failed, ", error_msg($obj);
    }

    unless ($opt{SubnetId}) {
        $self->{SubnetIdD} = $self->{SubnetId};
        delete $self->{SubnetId};
    }

    return $self;
}

sub create_internet_gateway
{
    my ($self) = @_;

    my $uri = $self->builduri("GET",
        "Action"  => "CreateInternetGateway");

    Debug::print("create internet gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create internet gateway $uri failed, ", error_msg($obj);
    }

    $self->{InternetGatewayId} = $obj->{internetGateway}->{internetGatewayId};
    return $self->tag(id => $self->{InternetGatewayId});
}

sub attach_internet_gateway
{
    my ($self, %opt) = @_;
    
    my $vpcid = $opt{VpcId} || $self->{VpcId};
    my $gwid = $opt{InternetGatewayId} || $self->{InternetGatewayId};

    my $uri = $self->builduri("GET",
        "Action"            => "AttachInternetGateway",
        "InternetGatewayId" => $gwid,
        "VpcId"             => $vpcid);

    Debug::print("attach internet gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "attach internet gateway $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub detach_internet_gateway
{
    my ($self, %opt) = @_;

    my $vpcid = $opt{VpcId} || $self->{VpcId};
    my $gwid  = $opt{InternetGatewayId} || $self->{InternetGatewayId};

    my $uri = $self->builduri("GET",
        "Action"            => "DetachInternetGateway",
        "InternetGatewayId" => $gwid,
        "VpcId"             => $vpcid);

    Debug::print("detach internat gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "detach internat gateway $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub describe_internet_gateway
{
    my ($self, %opt) = @_;

    my $gwid = $opt{InternetGatewayId} || $self->{InternetGatewayId};

    my $uri = $self->builduri("GET",
        "Action"            => "DescribeInternetGateways",
        "InternetGatewayId.1" => $gwid);

    Debug::print("describe internet gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe internet gateway $uri failed, ", error_msg($obj);
    }
    return $obj;
}
    
sub delete_internet_gateway
{
    my ($self, %opt) = @_;

    my $gwid = $opt{InternetGatewayId} || $self->{InternetGatewayId};

    my $uri = $self->builduri("GET",
        "Action"            => "DeleteInternetGateway",
        "InternetGatewayId" => $gwid);

    Debug::print("delete internat gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete internat gateway $uri failed, ", error_msg($obj);
    }

    unless ($opt{InternetGatewayId}) {
        $self->{InternetGatewayIdD} = $self->{InternetGatewayId};
        delete $self->{InternetGatewayId};
    }

    return $self;
}

sub create_vpn_gateway
{
    my ($self, %opt) = @_;
    my $type = $opt{Type} || "ipsec.1";

    my $uri = $self->builduri("GET",
        "Action" => "CreateVpnGateway",
        "Type"   => $type);
    
    Debug::print("create vpn gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create vpn gateway $uri failed, ", error_msg($obj);
    }
    
    $self->{VpnGatewayId} = $obj->{vpnGateway}->{vpnGatewayId};
    return $self->tag(id => $self->{VpnGatewayId});
}

sub describe_vpn_gateway
{
    my ($self, %opt) = @_;
    my $id = $opt{VpnGatewayId} || $self->{VpnGatewayId};
    
    my $uri = $self->builduri("GET",
        "Action" => "DescribeVpnGateways",
        "VpnGatewayId" => $id);

    Debug::print("describe vpn gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe vpn gateway $uri failed, ", error_msg($obj);
    }

    return $obj;
}

sub wait_vpn_gateway_available
{
    my ($self, %opt) = @_;
    unless (%opt) {
        $opt{VpnGatewayId} = $self->{VpnGatewayId} || $self->{VpnGateWayIdD};
    }

    my $func = sub {
        my $obj = $self->describe_vpn_gateway(%opt);
        return $obj->{vpnGatewaySet}->{item}->[0]->{state} eq "available";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait vpn gateway available timeout";
    }
    return $self;
}

sub attach_vpn_gateway
{
    my ($self, %opt) = @_;

    my $vpcid = $opt{VpcId} || $self->{VpcId};
    my $vgwid = $opt{VpnGatewayId} || $self->{VpnGatewayId};

    my $uri = $self->builduri("GET",
        "Action"       => "AttachVpnGateway",
        "VpnGatewayId" => $vgwid,
        "VpcId"        => $vpcid); 

    Debug::print("attach vpn gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "attach vpn gateway $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub wait_vpn_gateway_attached
{
    my ($self, %opt) = @_;
    
    my $func = sub {
        my $obj = $self->describe_vpn_gateway(%opt);
        return $obj->{vpnGatewaySet}->{item}->[0]
            ->{attachments}->{item}->[0]->{state} eq "attached";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait vpn gateway attached timeout";
    }
    return $self;
}

sub detach_vpn_gateway
{
    my ($self, %opt) = @_;
    
    my $vpcid = $opt{VpcId} || $self->{VpcId};
    my $vgwid = $opt{VpnGatewayId} || $self->{VpnGatewayId};

    my $uri = $self->builduri("GET",
        "Action"       => "DetachVpnGateway",
        "VpnGatewayId" => $vgwid,
        "VpcId"        => $vpcid);

    Debug::print("detach vpn gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "detach vpn gateway $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub wait_vpn_gateway_detached
{
    my ($self, %opt) = @_;
    
    my $func = sub {
        my $obj = $self->describe_vpn_gateway(%opt);
        my $state = $obj->{vpnGatewaySet}->{item}->[0]
                        ->{attachments}->{item}->[0]->{state};
        return $state ? $state eq "detached" : 1;
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait vpn gateway detached timeout";
    }
    return $self;
}

sub delete_vpn_gateway
{
    my ($self, %opt) = @_;

    my $vgwid = $opt{VpnGatewayId} || $self->{VpnGatewayId};

    my $uri = $self->builduri("GET",
        "Action"       => "DeleteVpnGateway",
        "VpnGatewayId" => $vgwid); 

    Debug::print("delete vpn gateway $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete vpn gateway $uri failed, ", error_msg($obj);
    }

    unless ($opt{VpnGatewayId}) {
        $self->{VpnGatewayIdD} = $self->{VpnGatewayId};
        delete $self->{VpnGatewayId};
    }
    return $self;
}

sub wait_vpn_gateway_deleted
{
    my ($self, %opt) = @_;
    unless (%opt) {
        $opt{VpnGatewayId} = $self->{VpnGatewayId} || $self->{VpnGatewayIdD};
    }

    my $func = sub {
        my $obj = $self->describe_vpn_gateway(%opt);
        my $state = $obj->{vpnGatewaySet}->{item}->[0]->{state};
        return $state ? $state eq "deleted" : 1;
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait vpn gateway deleted timeout";
    }
    return $self;
}

sub main_route_table
{
    my ($self, %opt) = @_;
    my $vpcid = $opt{VpcId} || $self->{VpcId};

    my $uri = $self->builduri("GET",
        "Action" => "DescribeRouteTables",
        "Filter.1.Name" => "vpc-id",
        "Filter.1.Value.1" => $vpcid,
        "Filter.2.Name" => "association.main",
        "Filter.2.Value.1" => "true");

    Debug::print("main route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "main route table $uri failed, ", error_msg($obj);
    }

    $self->{MainRouteTableId} = $obj->{routeTableSet}->{item}->[0]->{routeTableId};
    return $self->{MainRouteTableId};
}

sub create_route_table
{
    my ($self, %opt) = @_;
    my $vpcid = $opt{VpcId} || $self->{VpcId};

    my $uri = $self->builduri("GET",
        "Action" => "CreateRouteTable",
        "VpcId"  => $vpcid,
    );

    Debug::print("create route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create route table $uri failed, ", error_msg($obj);
    }

    $self->{RouteTableId} = $obj->{routeTable}->{routeTableId};
    return $self->tag(id => $self->{RouteTableId});
}

sub associate_route_table 
{
    my ($self, %opt) = @_;
    
    my $rid = $opt{RouteTableId} || $self->{RouteTableId};
    my $nid = $opt{SubnetId} || $self->{SubnetId};
    
    my $uri = $self->builduri("GET",
        "Action"       => "AssociateRouteTable",
        "RouteTableId" => $rid,
        "SubnetId"     => $nid);

    Debug::print("associate route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "associate route table $uri failed, ", error_msg($obj);
    }
    
    $self->{AssociationId} = $obj->{associationId};
    return $self;
}

sub disassociate_route_table
{
    my ($self, %opt) = @_;

    my $id = $opt{AssociationId} || $self->{AssociationId};

    my $uri = $self->builduri("GET",
        "Action"        => "DisassociateRouteTable",
        "AssociationId" => $id);

    Debug::print("disassociate route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "disassociate route table $uri failed, ", error_msg($obj);
    }

    unless ($opt{AssociationId}) {
        delete $self->{AssociationId};
    }

    return $self;
}

sub describe_route_table
{
    my ($self, %opt) = @_;
    my $id = $opt{RouteTableId} || $self->{RouteTableId};

    my $uri = $self->builduri("GET",
        "Action"    => "DescribeRouteTables",
        "RouteTableId.1" => $id);

    Debug::print("describe route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe route table $uri failed, ", error_msg($obj);
    }
    return $obj;
}

sub delete_route_table            
{
    my ($self, %opt) = @_;

    my $id = $opt{RouteTableId} || $self->{RouteTableId};

    my $uri = $self->builduri("GET",
        "Action" => "DeleteRouteTable",
        "RouteTableId" => $id);

    Debug::print("delete route table $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete route table $uri failed, ", error_msg($obj);
    }

    if ($opt{RouteTableId}) {
        $self->{RouteTableIdD} = $self->{RouteTableId};
        delete $self->{RouteTableId};
    }

    return $self;
}

sub default_route_to_igw
{
    my ($self, %opt) = @_;

    my $rtid = $opt{RouteTableId} || $self->{RouteTableId} || $self->main_route_table();
    my $gwid = $opt{GatewayId} || $self->{InternetGatewayId};

    return $self->create_route(
        RouteTableId => $rtid,
        DestinationCidrBlock => "0.0.0.0/0",
        GatewayId => $gwid);
}

sub create_route
{
    my ($self, %opt) = @_;
    
    my $rtid = $opt{RouteTableId} || $self->{RouteTableId};
    my $cidr = $opt{DestinationCidrBlock} or confess "DestinationCidrBlock required";

    my @optvar;
    if ($opt{GatewayId}) {
        push(@optvar, "GatewayId" => $opt{GatewayId});
    } elsif ($opt{InstanceId}) {
        push(@optvar, "InstanceId" => $opt{InstanceId});
    } elsif ($opt{NetworkInterfaceId}) {
        push(@optvar, "NetworkInterfaceId" => $opt{NetworkInterfaceId});
    } else {
        confess "GatewayId | InstanceId | NetworkInterfaceId required";
    }

    my $uri = $self->builduri("GET",
       "Action"               => "CreateRoute",
       "RouteTableId"         => $rtid,
       "DestinationCidrBlock" => uri_escape($cidr),
       @optvar);

    Debug::print("create route $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create route $uri failed, ", error_msg($obj);
    }

    return $self;
}

sub delete_route
{
    my ($self, %opt) = @_;

    my $rtid = $opt{RouteTableId} || $self->{RouteTableId};
    my $cidr = $opt{DestinationCidrBlock} or confess "DestinationCidrBlock required";

    my $uri = $self->builduri("GET",
        "Action"               => "DeleteRoute",
        "RouteTableId"         => $rtid,
        "DestinationCidrBlock" => uri_escape($cidr));

    Debug::print("delete route $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete route $uri failed, ", error_msg($obj);
    }

    return $self;
}

sub create_network_interface
{
    my ($self, %opt) = @_;

    my $SubnetId = $opt{SubnetId} || $self->{SubnetId};

    my @optvar;
    if ($opt{PrivateIpAddress}) {
        push(@optvar, "PrivateIpAddress" => $opt{PrivateIpAddress});
    }
    
    if ($opt{PrivateIpAddresses}) {
        my $i = 0;
        foreach my $o (@{$opt{PrivateIpAddresses}}) {
            push(@optvar, "PrivateIpAddresses.$i.PrivateIpAddress" =>
                 uri_escape($o->{PrivateIpAddress}));
            if ($o->{Primary}) {
                push(@optvar, "PrivateIpAddresses.$i.Primary" => $o->{Primary});
            }
        }
    }

    if ($opt{SecurityGroupId}) {
        push(@optvar, multi_kvs("SecurityGroupId", $opt{SecurityGroupId}));
    }

    if ($opt{Description}) {
        push(@optvar, "Description" => $opt{Description});
    }

    my $uri = $self->builduri("GET",
        "Action"   => "CreateNetworkInterface",
        "SubnetId" => $SubnetId,
        @optvar);

    Debug::print("create network interface $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create network interface $uri failed, ", error_msg($obj);
    }

    $self->{NetworkInterfaceId} = $obj->{networkInterface}->{networkInterfaceId};
    return $self->tag(id => $self->{NetworkInterfaceId});
}

sub attach_network_interface
{
    my ($self, %opt) = @_;
    
    my $NetworkInterfaceId = $opt{NetworkInterfaceId} || $self->{NetworkInterfaceId};
    my $InstanceId = $opt{InstanceId} || $self->{InstanceId};
    my $DeviceIndex = $opt{DeviceIndex} || 1;

    my $uri = $self->builduri("GET",
        "Action"             => "AttachNetworkInterface",
        "NetworkInterfaceId" => $NetworkInterfaceId,
        "InstanceId"         => $InstanceId,
        "DeviceIndex"        => $DeviceIndex);

    Debug::print("attach network interface $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "attach network interface $uri failed, ", error_msg($obj);
    }

    $self->{AttachmentId} = $obj->{attachmentId};
    return $self;
}

sub detach_network_interface
{
    my ($self, %opt) = @_;

    my $id = $opt{AttachmentId} ||$self->{AttachmentId};
    my $force = $opt{Force} || 0;

    my $uri = $self->builduri("GET",
        "Action" => "DetachNetworkInterface",
        "AttachmentId" => $id);

    Debug::print("detach network interface $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "detach network interface $uri failed, ", error_msg($obj);
    }

    if ($opt{AttachmentId}) {
        delete $self->{AttachmentId};
    }
    
    return $self;
}

sub delete_network_interface
{
    my ($self, %opt) = @_;

    my ($id, $key);
    if ($opt{NetworkInterfaceId}) {
        $id = $opt{NetworkInterfaceId};
    } else {
        $id = $self->{NetworkInterfaceId};
        $key = "NetworkInterfaceId";
    }

    my $uri = $self->builduri("GET",
        "Action" => "DeleteNetworkInterface",
        "NetworkInterfaceId" => $id);

    Debug::print("delete network interface $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete network interface $uri failed, ", error_msg($obj);
    }

    delete $self->{$key} if ($key);
    return $self;
}

sub allocate_address
{
    my ($self, %opt) = @_;

    my $domain = $opt{Domain} || $self->{Domain};
    
    my $uri = $self->builduri("GET",
        "Action" => "AllocateAddress",
        "Domain" => $domain);

    Debug::print("allocate address $uri");
    
    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "allocate address $uri failed, ", error_msg($obj);
    }

    $self->{PublicIp} = $obj->{publicIp};
    $self->{AllocationId} = $obj->{allocationId} if ($obj->{allocationId});

    Debug:print("EIP ", $self->{PublicIp});
    return $self;
}

sub associate_address
{
    my ($self, %opt) = @_;

    my @optvar;
    if ($opt{PublicIp} && $opt{InstanceId}) {
        push(@optvar, "PublicIp" => $opt{PublicIp});
        push(@optvar, "InstanceId" => $opt{InstanceId});
    } elsif ($opt{InstanceId} && $opt{AllocationId}) {
        push(@optvar, "AllocationId" => $opt{AllocationId});
        push(@optvar, "InstanceId" => $opt{InstanceId});
    } elsif ($opt{AllocationId} && $opt{NetworkInterfaceId}) {
        push(@optvar, "AllocationId" => $opt{AllocationId});
        push(@optvar, "NetworkInterfaceId" => $opt{NetworkInterfaceId});
        if ($opt{PrivateIpAddress}) {
            push(@optvar, "PrivateIpAddress" => $opt{PrivateIpAddress});
        }
    } elsif ($self->{Domain} eq "vpc") {
        push(@optvar, "AllocationId" => $self->{AllocationId});
        if ($self->{InstanceId}) {
            push(@optvar, "InstanceId" => $self->{InstanceId});
        } else {
            push(@optvar, "NetworkInterfaceId" => $self->{NetworkInterfaceId});
            if ($opt{PrivateIpAddress}) {
                push(@optvar, "PrivateIpAddress" => $opt{PrivateIpAddress});
            }
        }
    } else {
        push(@optvar, "PublicIp" => $self->{PublicIp},
             "InstanceId" => $self->{InstanceId});
    }

    my $uri = $self->builduri("GET",
        "Action" => "AssociateAddress",
        @optvar);

    Debug::print("associate address $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "associate address $uri failed, ", error_msg($obj);
    }

    if ($obj->{associationId}) {
        $self->{AssociationId} = $obj->{associationId};
    }
    return $self;
}

sub disassociate_address
{
    my ($self, %opt) = @_;

    my $key;
    my @optvar;
    if ($opt{PublicIp}) {
        my $ip = $opt{PublicIp} || $self->{PublicIp};
        push(@optvar, "PublicIp" => $ip);
        $key = "PublicIp";
    } elsif ($opt{AssociationId}) {
        my $id = $opt{AssociationId} || $self->{AssociationId};
        push(@optvar, "AssociationId" => $id);
        $key = "AssociationId";
    } elsif ($self->{Domain} eq "vpc") {
        push(@optvar, "AssociationId" => $self->{AssociationId});
    } else {
        push(@optvar, "PublicIp" => $self->{PublicIp});
    }

    my $uri = $self->builduri("GET",
        "Action" => "DisassociateAddress",
        @optvar);

    Debug::print("disassociate address $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "disassociate address $uri failed, ", error_msg($obj);
    }

    delete $self->{$key} if ($key);
    return $self;
}

sub release_address
{
    my ($self, %opt) = @_;
 
    my $key;
    my @optvar;
    if ($opt{PublicIp}) {
        push(@optvar, "PublicIp" => $opt{PublicIp});
    } elsif ($opt{AllocationId}) {
        push(@optvar, "AllocationId" => $opt{AllocationId});
    } elsif ($self->{Domain} eq "vpc") {
        push(@optvar, "AllocationId" => $self->{AllocationId});
        $key = $self->{AllocationId};
    } else {
        push(@optvar, "PublicIp" => $self->{PublicIp});
        $key = $self->{PublicIp};
    }

    my $uri = $self->builduri("GET",
        "Action" => "ReleaseAddress",
        @optvar);

    Debug::print("release address $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "release address $uri failed, ", error_msg($obj);
    }

    delete $self->{$key} if ($key);
    return $self;
}

sub multi_kv
{
    my ($key, $vs) = @_;

    my @kvs;
    if (ref($vs) eq "ARRAY") {
        my $i = 0;
        foreach my $id (@{$vs}) {
            $i++;
            push(@kvs, "$key.$i" => $id);
        }
    } else {
        push(@kvs, "$key.1" => $vs);
    }
    return @kvs;
}

sub run_instance
{
    my ($self, %opt) = @_;

    my $imageid = $opt{ImageId} or confess "image id required";
    my $type = $opt{InstanceType} || "t1.micro";
    my $keynam = $opt{KeyName} || $self->{KeyName};

    my @optvar;

    if ($opt{SecurityGroupId}) {
        push(@optvar, multi_kv("SecurityGroupId", $opt{SecurityGroupId}));
    }

    if ($self->{Domain} eq "vpc" && $opt{SubnetId}) {
        push(@optvar, "SubnetId" => $opt{SubnetId});
        if ($opt{PrivateIpAddress}) {
            push(@optvar, "PrivateIpAddress" => $opt{PrivateIpAddress});
        }
    }

    my $i;

    $i = 1;
    if ($opt{RootVolumeSize}) {
        my $obj = $self->describe_image(ImageId => $opt{ImageId});
        my $bdmap = $obj->{imagesSet}->{item}->[0]
          ->{blockDeviceMapping}->{item}->[0];
        my $dname = $bdmap->{deviceName};
        my $snapshot = $bdmap->{ebs}->{snapshotId};

        push(@optvar, "BlockDeviceMapping.$i.DeviceName", uri_escape($dname));
        push(@optvar, "BlockDeviceMapping.$i.Ebs.SnapshotId", $snapshot);
        push(@optvar, "BlockDeviceMapping.$i.Ebs.VolumeSize", $opt{RootVolumeSize});
    }
    
    if ($opt{BlockDeviceMapping}) {
        foreach my $o(@{$opt{BlockDeviceMapping}}) {
            $i++;
            if ($o->{"DeviceName"}) {
                push(@optvar, "BlockDeviceMapping.$i.DeviceName",
                     uri_escape($o->{"DeviceName"}));
            }
            if ($o->{"Ebs.VolumeSize"}) {
                push(@optvar, "BlockDeviceMapping.$i.Ebs.VolumeSize",
                     $o->{"Ebs.VolumeSize"});
            }
            if ($o->{"Ebs.DeleteOnTermination"}) {
                push(@optvar, "BlockDeviceMapping.$i.Ebs.DeleteOnTermination",
                     $o->{"Ebs.DeleteOnTermination"});
            }
            if ($o->{"Ebs.VolumeType"}) {
                push(@optvar, "BlockDeviceMapping.$i.Ebs.VolumeType",
                     $o->{"Ebs.VolumeType"});
            }
        }
    }

    $i = 0;    
    if ($opt{NetworkInterface}) {
        foreach my $o (@{$opt{NetworkInterface}}) {
            $i++;
            if ($o->{NetworkInterfaceId}) {
                push(@optvar,"NetworkInterface.$i.NetworkInterfaceId" =>
                     $o->{NetworkInterfaceId});
            }

            my $dindex = exists $o->{DeviceIndex} ? $o->{DeviceIndex} : ($i-1);
            push(@optvar, "NetworkInterface.$i.DeviceIndex" => $dindex);

            my $netid = $o->{SubnetId} || $self->{SubnetId};
            push(@optvar, "NetworkInterface.$i.SubnetId" => $netid);

            if ($o->{PrivateIpAddress}) {
                push(@optvar, "NetworkInterface.$i.PrivateIpAddress" =>
                         $o->{PrivateIpAddress});
            }

            if ($o->{PrivateIpAddresses}) {
                my $j = 0;
                foreach my $addr(@{$o->{PrivateIpAddresses}}) {
                    push(@optvar,
                         "NetworkInterface.$i.PrivateIpAddresses.$j.PrivateIpAddress" =>
                             uri_escape($addr->{PrivateIpAddress}));
                    if ($addr->{Primary}) {
                        push(@optvar, "NetworkInterface.$i.PrivateIpAddresses.$j.Primary"
                                 => "true");
                    }
                }
            }

            if ($o->{AssociatePublicIpAddress}) {
                push(@optvar, "NetworkInterface.$i.AssociatePublicIpAddress" =>
                     $o->{AssociatePublicIpAddress} ? "true" : "false");
            }
        }
    }

    my $uri = $self->builduri("GET",
        "Action"            => "RunInstances",
        "ImageId"           => $imageid,
        "KeyName"           => $keynam,
        "MinCount"          => 1,
        "MaxCount"          => 1,
        "InstanceType"      => $type,
        @optvar);

    Debug::print("create instance $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create instance $uri failed, ", error_msg($obj);
    }

    $self->{InstanceId} = $obj->{instancesSet}->{item}->[0]->{instanceId};
    $self->{PublicIp} = $obj->{instancesSet}->{item}->[0]->{ipAddress};
    return $self->tag(id => $self->{InstanceId});
}

sub describe_instance
{
    my ($self, %opt) = @_;
    my $iid = $opt{InstanceId} || $self->{InstanceId};

    my $uri = $self->builduri("GET",
        "Action"       => "DescribeInstances",
        "InstanceId.1" => $iid);

    Debug::print("describe instance $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe instance $uri failed, ", error_msg($obj);
    }

    return $obj;    
}

sub wait_instance_running
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_instance(%opt);
        my $item = $obj->{reservationSet}->{item}->[0]->{instancesSet}->{item}->[0];

        if ($item->{instanceState}->{name} eq "running") {
            $self->{PublicIp} = $item->{ipAddress} unless ($opt{InstanceId});
            return 1;
        } else {
            return 0;
        }
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait instance running timeout";
    }
    return $self;
}

sub start_instance
{
    my ($self, %opt) = @_;
    my $id = $opt{InstanceId} || $self->{InstanceId};

    my $uri = $self->builduri("GET",
        "Action"       => "StartInstances",
        "InstanceId.1" => $id);

    Debug::print("start instance $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "start instance $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub stop_instance
{
    my ($self, %opt) = @_;

    my $id = $opt{InstanceId} || $self->{InstanceId};
    my $force = $opt{Force} || 0;

    my $uri = $self->builduri("GET",
        "Action"       => "StopInstances",
        "InstanceId.1" => $id,
        "Force"        => $force);

    Debug::print("stop instance $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "stop instance $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub wait_instance_stopped
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj =  $self->describe_instance(%opt);
        my $item = $obj->{reservationSet}->{item}->[0]->{instancesSet}->{item}->[0];
        return $item->{instanceState}->{name} eq "stopped";
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait instance stopped timeout";
    }
    return $self;
}

sub terminate_instance
{
    my ($self, %opt) = @_;

    my $id = $opt{InstanceId} || $self->{InstanceId};

    my $uri = $self->builduri("GET",
        "Action"       => "TerminateInstances",
        "InstanceId.1" => $id);

    Debug::print("terminate instance $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "terminate instance $uri failed, ", error_msg($obj);
    };

    unless ($opt{InstanceId}) {
        $self->{InstanceIdD} = $self->{InstanceId};
        delete $self->{InstanceId};
    }
    return $self;
}

sub wait_instance_terminated
{
    my ($self, %opt) = @_;
    $opt{InstanceId} = $self->{InstanceId} || $self->{InstanceIdD} unless (%opt);

    my $func = sub {
        my $obj = $self->describe_instance(%opt);
        my $item = $obj->{reservationSet}->{item}->[0]->{instancesSet}->{item}->[0];
        return $item ? $item->{instanceState}->{name} eq "terminated" : 1;
    };

    unless (CloudUtil::loop_call($func)) {
        confess "wait instance terminated timeout";
    }
    return $self;
}

sub create_image
{
    my ($self, %opt) = @_;

    my $id = $opt{InstanceId} || $self->{InstanceId};
    my $name = $opt{Name} || $self->{name};

    my $uri = $self->builduri("GET",
        "Action" => "CreateImage",
        "InstanceId" => $id,
        "Name" => $name);

    Debug::print("create image $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create image $uri failed, ", error_msg($obj);
    }

    $self->{ImageId} = $obj->{imageId};
    return $self->tag(id => $self->{ImageId});
}

sub describe_image
{
    my ($self, %opt) = @_;
    my $id = $opt{ImageId} || $self->{ImageId};

    my $uri = $self->builduri("GET",
        "Action"    => "DescribeImages",
        "ImageId.1" => $id);

    Debug::print("describe image $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "describe image $uri failed, ", error_msg($obj);
    }
    return $obj;
}

sub wait_image_available
{
    my ($self, %opt) = @_;

    my $func = sub {
        my $obj = $self->describe_image(%opt);
        return $obj->{imagesSet}->{item}->[0]->{imageState} eq "available";
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
        "Action" => "DeregisterImage",
        "ImageId" => ($opt{ImageId} || $self->{ImageId}));

    Debug::print("delete image $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete image $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub make_image_public
{
    my ($self, %opt) = @_;

    my $uri = $self->builduri("GET",
        "Action" => "ModifyImageAttribute",
        "ImageId" => ($opt{ImageId} || $self->{ImageId}),
        "LaunchPermission.Add.1.Group" => "all");

    Debug::print("make image public $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "make image public $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub create_key_pair
{
    my ($self, %opt) = @_;
    my $name = $opt{KeyName} || $self->{name};

    my $uri = $self->builduri("GET",
        "Action" => "CreateKeyPair",
        "KeyName" => $name);
    
    Debug::print("create keypair $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create keypair $uri failed, ", error_msg($obj);
    }

    $self->{sshkey} = $obj->{keyMaterial};
    # use keypair name as keypair id
    $self->{KeyName} = $name;

    Debug::print("private key ", $self->{sshkey});
    return $self;
}

sub delete_key_pair
{
    my ($self, %opt) = @_;
    my $name = $opt{KeyName} || $self->{name};

    my $uri = $self->builduri("GET",
        "Action" => "DeleteKeyPair",
        "KeyName" => $name);
    
    Debug::print("delete keypair $uri");
    
    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete keypair $uri failed, ", error_msg($obj);
    }
    return $self;
}

sub default_security_group
{
    my ($self, %opt) = @_;
    my $vpcid = $opt{VpcId} || $self->{VpcId};
    
    my $uri = $self->builduri("GET",
        "Action" => "DescribeSecurityGroups",
        "Filter.1.Name" => "group-name",
        "Filter.1.Value.1" => "default",
        "Filter.2.Name" => "vpc-id",
        "Filter.2.Value.1" => $vpcid);

    Debug::print("default security group $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "default security group $uri failed, ", error_msg($uri);
    }

    $self->{DefaultSecurityGroupId} = $obj->{securityGroupInfo}->{item}->[0]->{groupId};
    return $self->{DefaultSecurityGroupId};
}

sub security_group_allow_ssh
{
    my ($self) = @_;
    
    return $self->authorize_security_group_ingress(
        GroupId       => $self->default_security_group(),
        IpPermissions => [{
            IpProtocol => "tcp",
            FromPort   => 22,
            ToPort     => 22,
            IpRanges => [{CidrIp => "0.0.0.0/0"}],
        }]);
}

sub create_security_group
{
    my ($self, %opt) = @_;

    my $name = $opt{GroupName} || $self->{name};
    my $desc = $opt{GroupDescription} || $name;
    
    my @vpcopt;
    if ($opt{VpcId}) {
        push(@vpcopt, "VpcId" => $opt{VpcId});
    } elsif ($self->{Domain} eq "vpc" && $self->{VpcId}) {
        push(@vpcopt, "VpcId" => $self->{VpcId});
    }

    my $uri = $self->builduri("GET",
        "Action"           => "CreateSecurityGroup",
        "GroupName"        => $name,
        "GroupDescription" => $desc,
        @vpcopt);

    Debug::print("create security group $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "create security group $uri failed, ", error_msg($obj);
    }

    $self->{SecurityGroupId} = $obj->{groupId} if ($obj->{groupId});
    $self->tag(id => $self->{SecurityGroupId});

    return $self->authorize_security_group_ingress(
        IpPermissions => [{
            IpProtocol => "tcp",
            FromPort   => 22,
            ToPort     => 22,
            IpRanges => [{CidrIp => "0.0.0.0/0"}],
        }]);
}

sub authorize_security_group_ingress
{
    my ($self, %opt) = @_;

    my $gid = $opt{GroupId} || $self->{SecurityGroupId} || $self->default_security_group();
    $opt{IpPermissions} or confess "IpPermissions required";
        
    my @vpcopt;
    
    my ($i, $j) = (0, 0);
    foreach my $o (@{$opt{IpPermissions}}) {
        $i++;

        my $proto = $o->{IpProtocol} || "tcp";
        my $fromp = $o->{FromPort}   || 22;
        my $top   = $o->{ToPort}     || $fromp;

        push(@vpcopt, "IpPermissions.$i.IpProtocol" => $proto);
        push(@vpcopt, "IpPermissions.$i.FromPort"   => $fromp);
        push(@vpcopt, "IpPermissions.$i.ToPort"     => $top);

        $o->{IpRanges} = [] unless ($o->{IpRanges});
        $j = 0;
        foreach my $cidr (@{$o->{IpRanges}}) {
            $j++;
            my $cidrip = uri_escape($cidr->{CidrIp});
            push(@vpcopt, "IpPermissions.$i.IpRanges.$j.CidrIp" => $cidrip);
        }

        $o->{Groups} = [] unless ($o->{Groups});
        $j = 0;
        foreach my $group (@{$o->{Groups}}) {
            $j++;
            if ($group->{GroupName}) {
                push(@vpcopt,
                     "IpPermissions.$i.Groups.$j.GroupName" => $group->{GroupName});
            }
            if ($group->{GroupId}) {
                push(@vpcopt, "IpPermissions.$i.Groups.$j.GroupId" => $group->{GroupId});
            }
            if ($group->{UserId}) {
                push(@vpcopt, "IpPermissions.$i.Groups.$j.UserId" => $group->{UserId});
            }
        }
    }

    my $uri = $self->builduri("GET",
        "Action"  => "AuthorizeSecurityGroupIngress",
        "GroupId" => $gid,
        @vpcopt); 
    
    Debug::print("authorize security group $uri");

    my $obj = httpcall(get => $uri);
    if (is_error($obj) && error_code($obj) ne "InvalidPermission.Duplicate") {
        confess "authorize security group $uri failed, ", error_msg($obj);
    }

    return $self;
}

sub delete_security_group
{
    my ($self, %opt) = @_;

    my @vpcopt;
    if ($opt{GroupName}) {
        push(@vpcopt, "GroupName" => $opt{GroupName});
    } elsif ($opt{GroupId}) {
        push(@vpcopt, "GroupId" => $opt{GroupId});
    } elsif ($self->{Domain} eq "vpc") {
        push(@vpcopt, "GroupId" => $self->{SecurityGroupId});
    } else {
        push(@vpcopt, "GroupName" => $self->{name});
    }

    my $uri = $self->builduri("GET",
        "Action" => "DeleteSecurityGroup",
        @vpcopt);
    
    Debug::print("delete security group $uri");
    
    my $obj = httpcall(get => $uri);
    if (is_error($obj)) {
        confess "delete security group $uri failed, ", error_msg($obj);
    }
    
    if ($opt{GroupName}) {
        delete $self->{GroupName};
    } elsif ($opt{GroupId}) {
        delete $self->{GroupId};
    }
    return $self;    
}

sub destroy_
{
    my $self = shift;
    my %filter = map {$_ => 1} @_;

    my $uri = $self->builduri("GET",
                              "Action"           => "DescribeTags",
                              "Filter.1.Name"    => "key",
                              "Filter.1.Value.1" => "Name",
                              "Filter.2.Name"    => "value",
                              "Filter.2.Value.1" => $self->{name});

    Debug::print("get resources: $uri");

    my $tagset = httpcall(get => $uri);
    if (is_error($tagset)) {
        confess "get resources $uri failed, ", error_msg($tagset);
    }

    my $items = $tagset->{tagSet}->{item};

    my @eips;
    if ($filter{eip}) {
        my @ids = grep {$_->{resourceType} eq "instance"} @$items;
        my @optvar;
        for (my $i = 1; $i < @ids+1; $i++) {
            push(@optvar, "Filter.1.Name" => "instance-id") if ($i == 1);
            push(@optvar, "Filter.1.Value.$i" => $ids[$i-1]->{resourceId});
        }
        if (@optvar) {
            $uri = $self->builduri("GET",
                                   "Action" => "DescribeAddresses",
                                   @optvar);
        
            Debug::print("describe address $uri");

            my $obj = httpcall(get => $uri);
            if (is_error($obj)) {
                confess "describe address $uri failed, ", error_msg($obj);
            }

            if ($obj->{addressesSet}) {
                push(@eips, @{$obj->{addressesSet}->{item}});
            }
        }
    }
    
    foreach my $o (@eips) {
        if ($o->{domain} eq "standard") {
            $self->disassociate_address(PublicIp => $o->{publicIp});
            $self->release_address(PublicIp => $o->{publicIp});
        } else {
            $self->disassociate_address(AssociationId => $o->{associationId});
            $self->release_address(AllocationId => $o->{allocationId});
        }
    }

    if ($filter{instance}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "instance");
            $self->terminate_instance(InstanceId => $o->{resourceId})
                ->wait_instance_terminated(InstanceId => $o->{resourceId});
        }
    }

    if ($filter{'route_table'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "route-table");
            my $id = $o->{resourceId};
                
            my $obj = $self->describe_route_table(RouteTableId => $id);
            my $ritems = $obj->{routeTableSet}->{item}->[0]
                ->{associationSet}->{item} || [];
                
            foreach my $ritem (@$ritems) {
                $self->disassociate_route_table(
                    AssociationId => $ritem->{routeTableAssociationId});
            }

            $self->delete_route_table(RouteTableId => $id);
        }
    }

    if ($filter{'network_interface'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "network-interface");
            $self->delete_network_interface(NetworkInterfaceId => $o->{resourceId});
        }
    }

    if ($filter{'subnet'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "subnet");
            $self->delete_subnet(SubnetId => $o->{resourceId});
        }
    }

    if ($filter{'internet_gateway'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "internet-gateway");
            my $id = $o->{resourceId};
            
            my $obj = $self->describe_internet_gateway(InternetGatewayId => $id);
            my $iitems = $obj->{internetGatewaySet}->{item}->[0]
                ->{attachmentSet}->{item} || [];
            foreach my $iitem (@$iitems) {
                $self->detach_internet_gateway(
                    VpcId => $iitem->{vpcId}, InternetGatewayId => $id);
            }
                
            $self->delete_internet_gateway(InternetGatewayId => $id);
        }
    }

    if ($filter{'vpn_gateway'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "vpn-gateway");
            my $id = $o->{resourceId};

            my $obj = $self->describe_vpn_gateway("VpnGatewayId" => $id);
            my $vitems = $obj->{vpnGatewaySet}->{item}->[0]
                ->{attachments}->{item} || [];
            foreach my $vitem (@{$vitems}) {
                $self->detach_vpn_gateway(
                    VpcId => $vitem->{vpcId}, VpnGatewayId => $id);
            }
                
            $self->delete_vpn_gateway(VpnGatewayId => $o->{resourceId});
        }
    }

    if ($filter{'security_group'}) {
        foreach my $o (@{$items}) {
            next unless ($o->{resourceType} eq "security-group");
            $self->delete_security_group(GroupId => $o->{resourceId});
        }
    }

    if ($filter{'vpc'}) {
        foreach my $o (@$items) {
            next unless ($o->{resourceType} eq "vpc");
            $self->delete_vpc(VpcId => $o->{resourceId});
        }
    }

    if ($filter{'key_pair'}) {
        $self->delete_key_pair(KeyName => $self->{name});
    }

    if ($filter{'image'}) {
        foreach my $o (@$items) {
            next unless ($o->{resourceType} eq "image");
            $self->delete_image(ImageId => $o->{resourceId});
        }
    }

    return $self;
}

sub destroy
{
    my $self = shift;
    
    for (my $i = 0;; $i++) {
        eval {
            # return in eval is syntax valid, but not semantics valid
            $self->destroy_(@_);
        } or do {
            if ($i < 3) {
                sleep(1);
                next;
            } else {
                confess $@;
            }
        };
        last;
    }
    return $self;
}

sub builduri
{
    my ($self, $method, %kv) = @_;
    my @qs = map { "$_=". $kv{$_} } keys(%kv);

    my $endpoint = $self->{endpoint};

    push(@qs, "AWSAccessKeyId=". $self->{akey});
    push(@qs, "Timestamp=". CloudUtil::iso8601());
    push(@qs, "Version=2013-10-15");
    push(@qs, "SignatureVersion=2");
    push(@qs, "SignatureMethod=HmacSHA256");
    push(@qs, "Signature=". $self->signature($method, $endpoint, "/", @qs));

    return "https://$endpoint?". join("&", @qs);
}

sub ec2_endpoint
{
    my ($region) = @_;
    return "ec2.$region.amazonaws.com";
}

sub signature
{
    my ($self, $method, $endpoint, $path, @qs) = @_;
    my $data = join("\n", $method, $endpoint, $path, join("&", sort @qs));
    my $digest = hmac_sha256_base64($data, $self->{skey});
    $digest .= "=" while (length($digest) % 4);
    return uri_escape($digest);
}

# class method
sub is_error
{
    my $obj = shift;
    return exists $obj->{Errors};
}

sub error_code
{
    my $obj = shift;
    return $obj->{Errors}->[0]->{Error}->{Code};
}    

sub error_msg
{
    my $obj = shift;
    return $obj->{Errors}->[0]->{Error}->{Code}. ":".
        $obj->{Errors}->[0]->{Error}->{Message};
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
        confess "http method required";
    }

    my $agent = LWP::UserAgent->new();
    my $request = HTTP::Request->new($method => $uri);
    my $response = $agent->request($request);

    my $body = $response->content();
    Debug::verbose("xml string $body");
    my $obj = eval { XMLin($body, ForceArray => ["Errors", "item"],
                           KeyAttr => '', SuppressEmpty => undef) };
    unless ($obj) {
        confess "AWS HTTP failed, ", Dumper($response);
    }

    Debug::verbose("xml obj ", Dumper($obj));
    return $obj;
}

1;
