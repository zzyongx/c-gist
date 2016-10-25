#!/usr/bin/perl

use strict;
use warnings;

my @sql;
my @ph;
my $n;
while (my $line = <>) {
  chomp $line;
  if ($line =~ /^UPDATE/) {
    @sql = split //, $line;

    $n = 0;
    my $i = 0;
    @ph = ();
    foreach (@sql) {
      if ($_ eq "?") {
        $n++;
        push(@ph, $i);
      }
      $i++;
    }
  } else {
    my @array = split /,\s/, $line;
    next if (@array != $n);

    my $i = 0;
    foreach my $e (@array) {
      $e =~ /(.+?)\((.+)\)/;

      my $value;
      if ($2 eq "Date") {
        $value = "'$1'";
      } else {
        $value = $1;
      }
      @sql[$ph[$i++]] = $value;
    }
    print join("", @sql, ";"), "\n";
  }
}
