package ImageDb;

use strict;
use warnings;

use JSON::PP qw(decode_json encode_json);
use Fcntl qw(:flock :seek);
use Data::Dumper;

sub new
{
    my ($class, $f) = @_;
    my $fh;
    if (-f $f) {
        open ($fh, "+<", $f) or die "$f open failed";
    } else {
        open ($fh, "+>", $f) or die "$f create failed";
    }
    bless {fh => $fh}, $class;
}

sub update 
{
    my $self = shift;
    my ($filter, $newpart) = @_;
    my $fh = $self->{fh};

    flock($fh, LOCK_EX);

    seek($fh, 0, SEEK_SET);

    local $/ = undef;
    my $c = <$fh>;
    my $obj = $c ? decode_json($c) : [];

    my $gmatch = 0;
    foreach my $o (@$obj) {
        my $rmatch = 1;
        while (my ($k, $v) = each (%$filter)) {
            unless (exists $o->{$k} && $o->{$k} eq $v) {
                $rmatch = 0;
                last;
            }
        }
        if ($rmatch) {
            while (my ($k, $v) = each (%$newpart)) {
                $o->{$k} = $v;
            }
            $gmatch = 1;
            last;
        }
    }

    unless ($gmatch) {
        my $record = $filter;
        while (my ($k, $v) = each (%$newpart)) {
            $record->{$k} = $v;
        }
        push (@$obj, $record);
    }

    truncate $fh, 0;
    seek($fh, 0, SEEK_SET);
    print $fh encode_json($obj);

    flock($fh, LOCK_UN);

    return $self;
}

sub get
{
    my $self = shift;
    my %filter = @_;
    my $fh = $self->{fh};

    flock($fh, LOCK_SH);

    seek($fh, 0, SEEK_SET);

    local $/ = undef;
    my $c = <$fh>;
    my $obj = $c ? decode_json($c) : [];

    flock($fh, LOCK_UN);

    return grep {
        my $o = $_;

        my $match = 1;
        while (my ($k, $v) = each (%filter)) {
            unless (exists $o->{$k} && $o->{$k} eq $v) {
                $match = 0;
                last;
            }
        }
        $match;
    } @$obj;
}

1;
