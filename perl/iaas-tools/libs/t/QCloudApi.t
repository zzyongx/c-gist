#!/usr/bin/perl

use strict;
use warnings;

use Carp;
use FindBin qw($Bin);
use lib "$Bin/..";

use QCloudApi;
use Debug;
$Debug::On = 1;

my $akey = $ENV{QCLOUD_AKEY} or confess "akey required";
my $skey = $ENV{QCLOUD_SKEY} or confess "skey required";
my $region = $ENV{QCLOUD_REGION} or confess "region required";

my $runall = $ENV{QCLOUD_RUNALL};

my $csp = QCloudApi->new(akey => $akey, skey => $skey,
                         region => $region, name => "QCloudApiTest");

# QCLOUD_CREATE_ROUTER=1
if ($ENV{QCLOUD_CREATE_ROUTER} || $runall) {
    my $sgid = $ENV{QCLOUD_SGID} or confess "required sgid";
    $csp->create_router(sgid => $sgid);
}

# QCLOUD_WAIT_INSTANCE_CHANGE_IP=1
if ($ENV{QCLOUD_WAIT_INSTANCE_CHANGE_IP} || $runall) {
   my $iid = $ENV{QCLOUD_IID} or confess "required iid";
   my $ip  = $ENV{QCLOUD_IP} or confess "required ip";

   $csp->wait_instance_change_ip(ip => $ip, iid => $iid);
}

print "OK\n";
   
