#!/usr/bin/perl

use strict;
use warnings;

use Carp;
use Data::Dumper;
use FindBin qw($Bin);
use lib "$Bin/..";

use AwsApi;
use Debug;
$Debug::On = 1;

# ENV INFO
# AKEY=_AKEY_  access key
# SKEY=_SKEY_  secret key
# REGION=_REGION_  region

while (my ($k, $v) = each(%ENV)) {
    print "$k:$v\n" if ($k =~ /^AWS/);
}

my $akey = $ENV{AWS_AKEY} or confess "akey required";
my $skey = $ENV{AWS_SKEY} or confess "skey required";
my $region = $ENV{AWS_REGION} or confess "region required";

my $runall = $ENV{AWS_RUNALL};

my $csp = AwsApi->new(akey => $akey, skey => $skey, 
                      region => $region, name => "AwsApiTest");

# AWS_CREATE_VPC=1 
if ($ENV{AWS_CREATE_VPC} || $runall) {
    $csp->create_vpc();
    $ENV{AWS_VPC_ID} = $csp->VpcId();
}

# AWS_CREATE_INTERNET_GATEWAY=1
if ($ENV{AWS_CREATE_INTERNET_GATEWAY} || $runall) {
    $csp->create_internet_gateway();

    $ENV{AWS_INTERNET_GATEWAY_ID} = $csp->InternetGatewayId();

    $csp->attach_internet_gateway(
        VpcId => $ENV{AWS_VPC_ID},
        InternetGatewayId => $ENV{AWS_INTERNET_GATEWAY_ID});
}

# AWS_CREATE_VPN_GATEWAY=1
if ($ENV{AWS_CREATE_VPN_GATEWAY} || $runall) {
    $csp->create_vpn_gateway()
        ->wait_vpn_gateway_available();
    
    $ENV{AWS_VPN_GATEWAY_ID} = $csp->VpnGatewayId();

    $csp->attach_vpn_gateway(VpcId => $ENV{AWS_VPC_ID})
        ->wait_vpn_gateway_attached();
}

# AWS_CREATE_ROUTE_TABLE=1
if ($ENV{AWS_CREATE_ROUTE_TABLE} || $runall) {
    $csp->create_route_table(VpcId => $ENV{AWS_VPC_ID});

    $csp->create_route(RouteTableId => $csp->RouteTableId(),
                       DestinationCidrBlock => "0.0.0.0/0",
                       GatewayId => $ENV{AWS_INTERNET_GATEWAY_ID});
    
    $ENV{AWS_ROUTE_TABLE_ID} = $csp->RouteTableId();
}

# AWS_CREATE_SUBNET=1
if ($ENV{AWS_CREATE_SUBNET} || $runall) {
    $csp->create_subnet(VpcId => $ENV{AWS_VPC_ID});
    $ENV{AWS_SUBNET_ID} = $csp->SubnetId();
}

# AWS_ASSOCIATE_ROUTE_TABLE=1
if ($ENV{AWS_ASSOCIATE_ROUTE_TABLE} || $runall) {
    $csp->associate_route_table(
        RouteTableId => $ENV{AWS_ROUTE_TABLE_ID},
        SubnetId => $ENV{AWS_SUBNET_ID});
    
    $ENV{AWS_ASSOCIATION_ID} = $csp->AssociationId();
    print "AssociationId ", $csp->AssociationId(), "\n";
}

# AWS_CREATE_SECURITY_GROUP=1  ./t/AwsApi.t
if ($ENV{AWS_CREATE_SECURITY_GROUP} || $runall) {
    $csp->create_security_group(VpcId => $ENV{AWS_VPC_ID});

    $ENV{AWS_SECURITY_GROUP_ID} = $csp->SecurityGroupId();
}

# AWS_CREATE_KEY_PAIR=1
if ($ENV{AWS_CREATE_KEY_PAIR} || $runall) {
    $csp->create_key_pair();
    $ENV{AWS_KEY_NAME} = $csp->KeyName();
}

# AWS_RUN_INSTANCE=1
if ($ENV{AWS_RUN_INSTANCE} || $runall) {
    my $imageid = $ENV{AWS_IMAGE_ID} or confess "imageid required";
    my $sgid = $ENV{AWS_SECURITY_GROUP_ID} or confess "security group id required";
    my $subnetid = $ENV{AWS_SUBNET_ID} or confess "subnet id required";

    my $privateip = "192.168.1.100";
    my $keypairid = $csp->name();

    $csp->run_instance(ImageId => $imageid,
                       SecurityGroupId => $sgid,
                       KeyName => $csp->name(),
                       SubnetId => $subnetid,
                       PrivateIpAddress => $privateip,
                       NetworkInterface => [
                           {AssociatePublicIpAddress => 1}
                       ]
        );
    $csp->wait_instance_running(InstanceId =>  $csp->InstanceId());
    $ENV{AWS_INSTANCE_ID} = $csp->InstanceId();
}

# AWS_CREATE_NETWORK_INTERFACE=1
if ($ENV{AWS_CREATE_NETWORK_INTERFACE} || $runall) {
    $csp->create_network_interface(
        SubnetId => $ENV{AWS_SUBNET_ID},
        PrivateIpAddresses => [
            {PrivateIpAddress => "192.168.1.11", Primary => 1},
            {PrivateIpAddress => "192.168.1.12"}
        ],
    );
}

# AWS_ALLOCATE_ADDRESS=1
if ($ENV{AWS_ALLOCATE_ADDRESS} || $runall) {
    $csp->allocate_address();

    $csp->associate_address(InstanceId => $ENV{AWS_INSTANCE_ID},
                            AllocationId => $csp->AllocationId());

    $ENV{AWS_PUBLIC_IP} = $csp->PublicIp();
    print "AllocationId ", $csp->AllocationId(), "\n";
}

# AWS_RESTART_INSTANCE=1
if ($ENV{AWS_RESTART_INSTANCE} || $runall) {
    my $id = $ENV{AWS_INSTANCE_ID} or confess "instance id required";
    $csp->stop_instance(InstanceId => $id)
        ->wait_instance_stopped(InstanceId => $id)
        ->start_instance(InstanceId => $id)
        ->wait_instance_running(InstanceId => $id);
}

# AWS_CREATE_IMAGE=1
if ($ENV{AWS_CREATE_IMAGE}) {
    my $id = $ENV{AWS_INSTANCE_ID} or confess "instance id required";
    $csp->create_image(InstanceId => $ENV{AWS_INSTANCE_ID})
        ->wait_image_available();
}

# AWS_DESCRIBE_INSTANCE=1
if ($ENV{AWS_DESCRIBE_INSTANCE}) {
    my $id = $ENV{AWS_INSTANCE_ID};
    $csp->describe_instance(InstanceId => $ENV{AWS_INSTANCE_ID});
}
# AWS_RELEASE_ADDRESS=1
if ($ENV{AWS_RELEASE_ADDRESS}) {
    $csp->release_address(AllocationId => $ENV{AWS_ALLOCATION_ID});
}

# AWS_DELETE_ROUTE=1
if ($ENV{AWS_DELETE_ROUTE}) {
    $csp->delete_route(
        RouteTableId => $ENV{AWS_ROUTE_TABLE_ID},
        DestinationCidrBlock => $ENV{AWS_DESTINATION_CIDR_BLOCK});
}

# AWS_DELETE_ROUTE_TABLE=1
if ($ENV{AWS_DELETE_ROUTE_TABLE}) {
    print Dumper($csp->describe_route_table(RouteTableId => $ENV{AWS_ROUTE_TABLE_ID}));

    if ($ENV{AWS_ASSOCIATION_ID}) {
	    $csp->disassociate_route_table(AssociationId => $ENV{AWS_ASSOCIATION_ID});
    }
    $csp->delete_route_table(RouteTableId => $ENV{AWS_ROUTE_TABLE_ID});
}

# AWS_DELETE_VPN_GATEWAY=1
if ($ENV{AWS_DELETE_VPN_GATEWAY}) {
    $csp->detach_vpn_gateway(
        VpcId => $ENV{AWS_VPC_ID},
        VpnGatewayId => $ENV{AWS_VPN_GATEWAY_ID})
        ->wait_vpn_gateway_detached(VpnGatewayId => $ENV{AWS_VPN_GATEWAY_ID});

    $csp->delete_vpn_gateway(VpnGatewayId => $ENV{AWS_VPN_GATEWAY_ID})
        ->wait_vpn_gateway_deleted(VpnGatewayId => $ENV{AWS_VPN_GATEWAY_ID});
}

# AWS_DELETE_INTERNET_GATEWAY=1
if ($ENV{AWS_DELETE_INTERNET_GATEWAY}) {
    $csp->detach_internet_gateway(
        VpcId  => $ENV{AWS_VPC_ID},
        InternetGatewayId => $ENV{AWS_INTERNET_GATEWAY_ID});

    $csp->delete_internet_gateway(InternetGatewayId => $ENV{AWS_INTERNET_GATEWAY_ID});
}

# AWS_DISASSOCIATE_ROUTE_TABLE=1
if ($ENV{AWS_DISASSOCIATE_ROUTE_TABLE}) {
    $csp->disassociate_route_table(AssociationId => $ENV{AWS_ASSOCIATION_ID});
}

# AWS_DELETE_SUBNET=1
if ($ENV{AWS_DELETE_SUBNET} || $runall) {
    $csp->delete_subnet(SubnetId => $ENV{AWS_SUBNET_ID});
}

# AWS_DELETE_SECURITY_GROUP=1
if ($ENV{AWS_DELETE_SECURITY_GROUP} || $runall) {
    $csp->delete_security_group(GroupId => $ENV{AWS_SECURITY_GROUP_ID});
}

# AWS_DELETE_KEY_PAIR=1
if ($ENV{AWS_DELETE_KEY_PAIR} || $runall) {
    $csp->delete_key_pair(KeyName => $ENV{AWS_KEY_NAME});
}

# AWS_DELETE_VPC=1
if ($ENV{AWS_DELETE_VPC} || $runall) {
    $csp->delete_vpc(VpcId => $ENV{AWS_VPC_ID});
}

# AWS_DESTROY=1
if ($ENV{AWS_DESTROY}) {
    $csp->destroy("vpc", "security_group", "keypair", "internet_gateway",
                  "network_interface", "subnet", "vpn_getway", "route_table",
                  "instance");
}

print "OK\n";
