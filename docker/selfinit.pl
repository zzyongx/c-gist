#!/usr/bin/perl

use strict;
use warnings;

use POSIX qw(pause);

#
# HAS BUG

my @starts = (
  "/usr/local/cfs/bin/cfs start",
);

my @stops = (
  "/usr/local/cfs/bin/cfs stop",
);

$SIG{TERM} = sub {};

foreach my $cmd (@starts) {
  spawn($cmd);
}

pause();

foreach my $cmd (@stops) {
  spawn($cmd);
}

exit(0);

sub spawn
{
  my ($cmd) = @_;
  my $pid = fork();
  if ($pid == 0) {
    print "run $cmd\n";
    exec($cmd);
    exit(0);
  }
}
